-- PostgreSQL triggers and stored procedures for WASAC billing
-- Each block is separated by -- @split (parsed by DatabaseRoutineInitializer)

-- Fires AFTER INSERT on bills: creates in-app notification for bill generation
CREATE OR REPLACE FUNCTION fn_notify_on_bill_insert()
RETURNS TRIGGER AS $$
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
        'Dear ' || v_customer_name || ', Your ' || NEW.billing_month || '/' || NEW.billing_year
            || ' ' || v_meter_type || ' utility bill of ' || NEW.total_amount
            || ' FRW has been successfully processed.',
        'BILL_GENERATED',
        NEW.id,
        FALSE,
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- @split
DROP TRIGGER IF EXISTS trg_bill_insert_notification ON bills;
CREATE TRIGGER trg_bill_insert_notification
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE FUNCTION fn_notify_on_bill_insert();
-- @split
-- Fires AFTER INSERT on payments when outstanding balance reaches zero
CREATE OR REPLACE FUNCTION fn_handle_full_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_outstanding   NUMERIC(19, 2);
    v_customer_id   BIGINT;
    v_customer_name VARCHAR(255);
    v_month         INTEGER;
    v_year          INTEGER;
BEGIN
    SELECT b.outstanding_balance, b.customer_id, b.billing_month, b.billing_year, c.full_name
    INTO v_outstanding, v_customer_id, v_month, v_year, v_customer_name
    FROM bills b
    JOIN customers c ON c.id = b.customer_id
    WHERE b.id = NEW.bill_id;

    IF v_outstanding IS NOT NULL AND v_outstanding <= 0 THEN
        UPDATE bills SET status = 'PAID' WHERE id = NEW.bill_id;

        INSERT INTO notifications (customer_id, message, event_type, reference_id, email_sent, created_at)
        VALUES (
            v_customer_id,
            'Dear ' || v_customer_name || ', your ' || v_month || '/' || v_year
                || ' payment has been received. Balance: 0 FRW.',
            'BILL_FULLY_PAID',
            NEW.bill_id,
            FALSE,
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- @split
DROP TRIGGER IF EXISTS trg_payment_full_notification ON payments;
CREATE TRIGGER trg_payment_full_notification
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE FUNCTION fn_handle_full_payment();
-- @split
CREATE OR REPLACE PROCEDURE sp_record_payment(
    p_bill_id BIGINT,
    p_amount NUMERIC(19, 2),
    p_method VARCHAR(50),
    p_payment_date DATE
)
LANGUAGE plpgsql
AS $$
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
$$;
-- @split
CREATE OR REPLACE PROCEDURE sp_apply_late_penalty(p_bill_id BIGINT, p_penalty_rate NUMERIC(8, 4))
LANGUAGE plpgsql
AS $$
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
$$;
