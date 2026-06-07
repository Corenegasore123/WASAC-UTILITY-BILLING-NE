package com.ne.wasac.config;

import java.util.List;

/**
 * PostgreSQL trigger functions and stored procedures for WASAC billing.
 * Kept in Java (not SQL files) so the JDBC driver sends each statement as-is.
 */
public final class DatabaseRoutines {

    private DatabaseRoutines() {
    }

    public static final List<String> STATEMENTS = List.of(
            """
            ALTER TABLE app_users
            ADD COLUMN IF NOT EXISTS pending_email_verification boolean NOT NULL DEFAULT false
            """,
            """
            UPDATE app_users SET national_id = '1199087766554401'
            WHERE email = 'corenegasore@gmail.com'
              AND (national_id IS NULL OR national_id = '')
            """,
            """
            UPDATE app_users u SET pending_email_verification = true
            WHERE u.status = 'INACTIVE' AND u.pending_email_verification = false
              AND u.customer_id IS NOT NULL
              AND EXISTS (
                  SELECT 1 FROM customers c
                  WHERE c.id = u.customer_id AND c.status = 'INACTIVE'
              )
            """,
            "ALTER TABLE otp_verifications DROP CONSTRAINT IF EXISTS otp_verifications_purpose_check",
            """
            ALTER TABLE otp_verifications ADD CONSTRAINT otp_verifications_purpose_check CHECK (purpose IN (
                'REGISTRATION', 'PASSWORD_RESET'
            ))
            """,
            "ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_type_check",
            """
            ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_type_check CHECK (action_type IN (
                'USER_CREATED', 'USER_DELETED', 'USER_ACTIVATED', 'USER_DEACTIVATED', 'USER_ROLE_CHANGED',
                'PROFILE_UPDATED', 'METER_READING_CAPTURED', 'BILL_APPROVED', 'BILL_PENALTY_APPLIED',
                'PAYMENT_RECORDED', 'TARIFF_CREATED', 'TARIFF_UPDATED', 'METER_STATUS_CHANGED',
                'CUSTOMER_STATUS_CHANGED'
            ))
            """,
            """
            CREATE OR REPLACE FUNCTION fn_bill_notification_message(
                p_customer_name VARCHAR,
                p_meter_type    VARCHAR,
                p_amount        NUMERIC
            )
            RETURNS TEXT AS $fn$
            BEGIN
                RETURN 'Dear ' || p_customer_name || ', Your ' || p_meter_type
                    || ' utility bill of ' || p_amount || ' FRW has been successfully processed.';
            END;
            $fn$ LANGUAGE plpgsql
            """,
            """
            CREATE OR REPLACE FUNCTION fn_notify_on_bill_insert()
            RETURNS TRIGGER AS $fn$
            DECLARE
                v_customer_name VARCHAR(255);
                v_meter_type    VARCHAR(20);
            BEGIN
                SELECT c.full_name, m.meter_type
                INTO v_customer_name, v_meter_type
                FROM customers c
                JOIN meters m ON m.id = NEW.meter_id
                WHERE c.id = NEW.customer_id;

                INSERT INTO notifications (customer_id, message, event_type, reference_id, email_sent, created_at)
                VALUES (
                    NEW.customer_id,
                    fn_bill_notification_message(v_customer_name, v_meter_type, NEW.total_amount),
                    'BILL_GENERATED',
                    NEW.id,
                    FALSE,
                    NOW()
                );
                RETURN NEW;
            END;
            $fn$ LANGUAGE plpgsql
            """,
            "DROP TRIGGER IF EXISTS trg_bill_insert_notification ON bills",
            """
            CREATE TRIGGER trg_bill_insert_notification
                AFTER INSERT ON bills
                FOR EACH ROW
                EXECUTE FUNCTION fn_notify_on_bill_insert()
            """,
            """
            CREATE OR REPLACE FUNCTION fn_handle_full_payment()
            RETURNS TRIGGER AS $fn$
            DECLARE
                v_outstanding   NUMERIC(19, 2);
                v_customer_id   BIGINT;
                v_customer_name VARCHAR(255);
                v_meter_type    VARCHAR(20);
                v_total_amount  NUMERIC(19, 2);
            BEGIN
                SELECT b.outstanding_balance, b.customer_id, b.total_amount, c.full_name, m.meter_type
                INTO v_outstanding, v_customer_id, v_total_amount, v_customer_name, v_meter_type
                FROM bills b
                JOIN customers c ON c.id = b.customer_id
                JOIN meters m ON m.id = b.meter_id
                WHERE b.id = NEW.bill_id;

                IF v_outstanding IS NOT NULL AND v_outstanding <= 0 THEN
                    UPDATE bills SET status = 'PAID' WHERE id = NEW.bill_id;

                    INSERT INTO notifications (customer_id, message, event_type, reference_id, email_sent, created_at)
                    VALUES (
                        v_customer_id,
                        fn_bill_notification_message(v_customer_name, v_meter_type, v_total_amount),
                        'BILL_FULLY_PAID',
                        NEW.bill_id,
                        FALSE,
                        NOW()
                    );
                END IF;
                RETURN NEW;
            END;
            $fn$ LANGUAGE plpgsql
            """,
            "DROP TRIGGER IF EXISTS trg_payment_full_notification ON payments",
            """
            CREATE TRIGGER trg_payment_full_notification
                AFTER INSERT ON payments
                FOR EACH ROW
                EXECUTE FUNCTION fn_handle_full_payment()
            """,
            """
            CREATE OR REPLACE PROCEDURE sp_record_payment(
                p_bill_id BIGINT,
                p_amount NUMERIC(19, 2),
                p_method VARCHAR(50),
                p_payment_date DATE
            )
            LANGUAGE plpgsql
            AS $proc$
            DECLARE
                v_outstanding NUMERIC(19, 2);
                v_total       NUMERIC(19, 2);
                v_paid        NUMERIC(19, 2);
            BEGIN
                SELECT outstanding_balance, total_amount, paid_amount
                INTO v_outstanding, v_total, v_paid
                FROM bills WHERE id = p_bill_id FOR UPDATE;

                IF v_outstanding IS NULL THEN
                    RAISE EXCEPTION 'Bill not found: %', p_bill_id;
                END IF;
                IF p_amount > v_outstanding THEN
                    RAISE EXCEPTION 'Payment exceeds outstanding balance';
                END IF;

                v_paid := v_paid + p_amount;
                v_outstanding := v_total - v_paid;

                UPDATE bills
                SET paid_amount = v_paid,
                    outstanding_balance = v_outstanding,
                    status = CASE WHEN v_outstanding <= 0 THEN 'PAID' ELSE 'PARTIALLY_PAID' END
                WHERE id = p_bill_id;

                INSERT INTO payments (bill_id, amount_paid, payment_method, payment_date)
                VALUES (p_bill_id, p_amount, p_method, p_payment_date);
            END;
            $proc$
            """,
            """
            CREATE OR REPLACE PROCEDURE sp_apply_late_penalty(p_bill_id BIGINT, p_penalty_rate NUMERIC(8, 4))
            LANGUAGE plpgsql
            AS $proc$
            DECLARE
                v_penalty NUMERIC(19, 2);
                v_before  NUMERIC(19, 2);
            BEGIN
                SELECT amount_before_tax INTO v_before FROM bills WHERE id = p_bill_id FOR UPDATE;
                v_penalty := ROUND(v_before * p_penalty_rate / 100, 2);
                UPDATE bills
                SET penalty_amount = penalty_amount + v_penalty,
                    total_amount = total_amount + v_penalty,
                    outstanding_balance = outstanding_balance + v_penalty
                WHERE id = p_bill_id;
            END;
            $proc$
            """,
            """
            CREATE OR REPLACE PROCEDURE sp_notify_overdue_bills()
            LANGUAGE plpgsql
            AS $proc$
            DECLARE
                overdue_bill CURSOR FOR
                    SELECT b.id, b.customer_id, b.total_amount, c.full_name, m.meter_type
                    FROM bills b
                    JOIN customers c ON c.id = b.customer_id
                    JOIN meters m ON m.id = b.meter_id
                    WHERE b.status IN ('APPROVED', 'PARTIALLY_PAID')
                      AND b.outstanding_balance > 0
                      AND b.due_date < CURRENT_DATE;
                v_bill_id       BIGINT;
                v_customer_id   BIGINT;
                v_total_amount  NUMERIC(19, 2);
                v_customer_name VARCHAR(255);
                v_meter_type    VARCHAR(20);
            BEGIN
                OPEN overdue_bill;
                LOOP
                    FETCH overdue_bill INTO v_bill_id, v_customer_id, v_total_amount, v_customer_name, v_meter_type;
                    EXIT WHEN NOT FOUND;

                    INSERT INTO notifications (customer_id, message, event_type, reference_id, email_sent, created_at)
                    VALUES (
                        v_customer_id,
                        fn_bill_notification_message(v_customer_name, v_meter_type, v_total_amount),
                        'PAYMENT_OVERDUE',
                        v_bill_id,
                        FALSE,
                        NOW()
                    )
                    ON CONFLICT ON CONSTRAINT uk_notification_event DO NOTHING;
                END LOOP;
                CLOSE overdue_bill;
            END;
            $proc$
            """
    );
}
