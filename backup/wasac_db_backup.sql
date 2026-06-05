--
-- PostgreSQL database dump
--

\restrict mw8opRKAwgHxXApziswBklIRcuuj1xGc00KoFnfRSrGopWYXKztKduhHonr2IaN

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: fn_handle_full_payment(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_handle_full_payment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_handle_full_payment() OWNER TO postgres;

--
-- Name: fn_notify_on_bill_insert(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_notify_on_bill_insert() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_notify_on_bill_insert() OWNER TO postgres;

--
-- Name: sp_apply_late_penalty(bigint, numeric); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.sp_apply_late_penalty(IN p_bill_id bigint, IN p_penalty_rate numeric)
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


ALTER PROCEDURE public.sp_apply_late_penalty(IN p_bill_id bigint, IN p_penalty_rate numeric) OWNER TO postgres;

--
-- Name: sp_record_payment(bigint, numeric, character varying, date); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.sp_record_payment(IN p_bill_id bigint, IN p_amount numeric, IN p_method character varying, IN p_payment_date date)
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


ALTER PROCEDURE public.sp_record_payment(IN p_bill_id bigint, IN p_amount numeric, IN p_method character varying, IN p_payment_date date) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: app_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.app_users (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    must_change_password boolean NOT NULL,
    password character varying(255) NOT NULL,
    phone_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    customer_id bigint,
    national_id character varying(255),
    pending_email_verification boolean DEFAULT false NOT NULL,
    CONSTRAINT app_users_full_name_check CHECK (((full_name)::text ~ '^[a-zA-Z\s]+$'::text)),
    CONSTRAINT app_users_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);


ALTER TABLE public.app_users OWNER TO postgres;

--
-- Name: app_users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.app_users ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.app_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_logs (
    id bigint NOT NULL,
    action_type character varying(255) NOT NULL,
    entity_id bigint,
    entity_name character varying(255) NOT NULL,
    new_value character varying(1000),
    old_value character varying(1000),
    performed_at timestamp(6) without time zone NOT NULL,
    user_id bigint,
    CONSTRAINT audit_logs_action_type_check CHECK (((action_type)::text = ANY ((ARRAY['USER_CREATED'::character varying, 'USER_DELETED'::character varying, 'USER_ACTIVATED'::character varying, 'USER_DEACTIVATED'::character varying, 'USER_ROLE_CHANGED'::character varying, 'PROFILE_UPDATED'::character varying, 'METER_READING_CAPTURED'::character varying, 'BILL_APPROVED'::character varying, 'PAYMENT_RECORDED'::character varying, 'TARIFF_CREATED'::character varying, 'TARIFF_UPDATED'::character varying, 'METER_STATUS_CHANGED'::character varying, 'CUSTOMER_STATUS_CHANGED'::character varying])::text[])))
);


ALTER TABLE public.audit_logs OWNER TO postgres;

--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.audit_logs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: bills; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.bills (
    id bigint NOT NULL,
    amount_before_tax numeric(19,2) NOT NULL,
    approved_at timestamp(6) without time zone,
    bill_reference character varying(255) NOT NULL,
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    consumption numeric(19,2) NOT NULL,
    due_date date NOT NULL,
    outstanding_balance numeric(19,2) NOT NULL,
    paid_amount numeric(19,2) NOT NULL,
    penalty_amount numeric(19,2) NOT NULL,
    status character varying(255) NOT NULL,
    tax_amount numeric(19,2) NOT NULL,
    total_amount numeric(19,2) NOT NULL,
    approved_by bigint,
    customer_id bigint NOT NULL,
    meter_id bigint NOT NULL,
    meter_reading_id bigint NOT NULL,
    CONSTRAINT bills_status_check CHECK (((status)::text = ANY ((ARRAY['UNPAID'::character varying, 'APPROVED'::character varying, 'PARTIALLY_PAID'::character varying, 'PAID'::character varying])::text[])))
);


ALTER TABLE public.bills OWNER TO postgres;

--
-- Name: bills_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.bills ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.bills_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customers (
    id bigint NOT NULL,
    address character varying(255) NOT NULL,
    date_of_birth date,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    national_id character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT customers_full_name_check CHECK (((full_name)::text ~ '^[a-zA-Z\s]+$'::text)),
    CONSTRAINT customers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'SUSPENDED'::character varying])::text[])))
);


ALTER TABLE public.customers OWNER TO postgres;

--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.customers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: meter_readings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.meter_readings (
    id bigint NOT NULL,
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    current_reading numeric(19,2) NOT NULL,
    previous_reading numeric(19,2) NOT NULL,
    reading_date date NOT NULL,
    meter_id bigint NOT NULL
);


ALTER TABLE public.meter_readings OWNER TO postgres;

--
-- Name: meter_readings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.meter_readings ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.meter_readings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: meters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.meters (
    id bigint NOT NULL,
    installation_date date NOT NULL,
    meter_number character varying(255) NOT NULL,
    meter_type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    customer_id bigint NOT NULL,
    CONSTRAINT meters_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT meters_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying, 'DISCONNECTED'::character varying])::text[])))
);


ALTER TABLE public.meters OWNER TO postgres;

--
-- Name: meters_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.meters ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.meters_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email_sent boolean NOT NULL,
    event_type character varying(255) NOT NULL,
    message character varying(500) NOT NULL,
    reference_id bigint,
    customer_id bigint NOT NULL
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.notifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: otp_verifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.otp_verifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(255) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    otp_hash character varying(255) NOT NULL,
    purpose character varying(255) NOT NULL,
    used boolean NOT NULL,
    CONSTRAINT otp_verifications_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['REGISTRATION'::character varying, 'PASSWORD_RESET'::character varying])::text[])))
);


ALTER TABLE public.otp_verifications OWNER TO postgres;

--
-- Name: otp_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.otp_verifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.otp_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    amount_paid numeric(19,2) NOT NULL,
    payment_date date NOT NULL,
    payment_method character varying(255) NOT NULL,
    bill_id bigint NOT NULL,
    CONSTRAINT payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['MOMO'::character varying, 'BANK'::character varying, 'CARD'::character varying, 'CASH'::character varying])::text[])))
);


ALTER TABLE public.payments OWNER TO postgres;

--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.payments ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT roles_name_check CHECK (((name)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_OPERATOR'::character varying, 'ROLE_FINANCE'::character varying, 'ROLE_CUSTOMER'::character varying])::text[])))
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.roles ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tariff_plans; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariff_plans (
    id bigint NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    fixed_service_charge numeric(19,2) NOT NULL,
    flat_rate_per_unit numeric(19,2),
    late_penalty_rate numeric(8,4) NOT NULL,
    meter_type character varying(255) NOT NULL,
    tariff_type character varying(255) NOT NULL,
    vat_rate numeric(8,4) NOT NULL,
    version_no integer NOT NULL,
    CONSTRAINT tariff_plans_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT tariff_plans_tariff_type_check CHECK (((tariff_type)::text = ANY ((ARRAY['FLAT'::character varying, 'TIERED'::character varying])::text[])))
);


ALTER TABLE public.tariff_plans OWNER TO postgres;

--
-- Name: tariff_plans_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tariff_plans ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tariff_plans_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tariff_tiers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tariff_tiers (
    id bigint NOT NULL,
    max_unit numeric(19,2) NOT NULL,
    min_unit numeric(19,2) NOT NULL,
    rate_per_unit numeric(19,2) NOT NULL,
    tariff_plan_id bigint NOT NULL
);


ALTER TABLE public.tariff_tiers OWNER TO postgres;

--
-- Name: tariff_tiers_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.tariff_tiers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tariff_tiers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL
);


ALTER TABLE public.user_roles OWNER TO postgres;

--
-- Data for Name: app_users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.app_users (id, email, full_name, must_change_password, password, phone_number, status, customer_id, national_id, pending_email_verification) FROM stdin;
4	hakizaroza@gmail.com	Marie Finance	f	$2a$10$jA4VqblD85Ed.PtlQSXtoO1vZPfG/FScgmWyflKvd65rEvJUXhpRS	0788987654	ACTIVE	\N	1199887766554432	f
3	gasoreyne08@gmail.com	Jean Operator	f	$2a$10$nf6JquXNVQyNPv1U0hc42uuuGBYZkf6AeoTnNFBqTMvhWwWFqZ3MS	0788134356	ACTIVE	\N	1199887766554433	f
2	constancemunyaneza@gmail.com	Constance Munyaneza	f	$2a$10$NhIPRZi0bppF7sflFl4iJeEJaLTkxsORNpq1Tjt3r.AfzzuJ91WgS	0782345678	ACTIVE	1	\N	f
1	corenegasore@gmail.com	Corene Gasore	f	$2a$10$qLVOH5gWcFUEuU7TyhZyMue2LedsYqv5Ijtc5MWrW9thPBlb1b35u	0729023495	ACTIVE	\N	1199087766554401	f
\.


--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, action_type, entity_id, entity_name, new_value, old_value, performed_at, user_id) FROM stdin;
1	PROFILE_UPDATED	2	AppUser	Constance Munyaneza	Constance Munyaneza	2026-06-05 12:34:21.175764	2
2	USER_ROLE_CHANGED	2	AppUser	ROLE_OPERATOR	ROLE_CUSTOMER	2026-06-05 12:38:12.043735	1
3	USER_ROLE_CHANGED	2	AppUser	ROLE_CUSTOMER	ROLE_OPERATOR	2026-06-05 12:38:28.830551	1
4	USER_DEACTIVATED	2	AppUser	INACTIVE	ACTIVE	2026-06-05 12:39:43.253368	1
5	USER_ACTIVATED	2	AppUser	ACTIVE	INACTIVE	2026-06-05 12:39:58.85518	1
6	USER_CREATED	3	AppUser	ROLE_OPERATOR	\N	2026-06-05 12:54:16.941929	1
7	USER_CREATED	4	AppUser	ROLE_FINANCE	\N	2026-06-05 12:55:28.980548	1
8	USER_DEACTIVATED	2	AppUser	INACTIVE	ACTIVE	2026-06-05 13:15:15.01607	1
9	USER_ACTIVATED	2	AppUser	ACTIVE	INACTIVE	2026-06-05 13:24:38.637618	1
10	TARIFF_CREATED	1	TariffPlan	1	\N	2026-06-05 13:38:25.315936	1
11	TARIFF_CREATED	2	TariffPlan	1	\N	2026-06-05 13:41:24.380782	1
16	METER_READING_CAPTURED	5	MeterReading	45	\N	2026-06-05 13:50:43.379164	3
17	METER_READING_CAPTURED	6	MeterReading	120	\N	2026-06-05 13:51:27.219722	3
18	BILL_APPROVED	1	Bill	APPROVED	UNPAID	2026-06-05 13:56:51.660404	4
19	PAYMENT_RECORDED	1	Payment	28910	\N	2026-06-05 14:02:49.268467	4
\.


--
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.bills (id, amount_before_tax, approved_at, bill_reference, billing_month, billing_year, consumption, due_date, outstanding_balance, paid_amount, penalty_amount, status, tax_amount, total_amount, approved_by, customer_id, meter_id, meter_reading_id) FROM stdin;
2	18600.00	\N	BILL-4A13E8B3	6	2026	120.00	2026-07-05	21948.00	0.00	0.00	UNPAID	3348.00	21948.00	\N	1	2	6
1	24500.00	2026-06-05 13:56:51.648684	BILL-CE5297C6	6	2026	45.00	2026-07-05	0.00	28910.00	0.00	PAID	4410.00	28910.00	4	1	1	5
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customers (id, address, date_of_birth, email, full_name, national_id, phone, status) FROM stdin;
1	Kigali, Rwanda	1995-01-15	constancemunyaneza@gmail.com	Constance Munyaneza	1199887766558800	0782345678	ACTIVE
\.


--
-- Data for Name: meter_readings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meter_readings (id, billing_month, billing_year, current_reading, previous_reading, reading_date, meter_id) FROM stdin;
5	6	2026	45.00	0.00	2026-06-01	1
6	6	2026	120.00	0.00	2026-06-01	2
\.


--
-- Data for Name: meters; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meters (id, installation_date, meter_number, meter_type, status, customer_id) FROM stdin;
1	2026-01-15	WM-00001	WATER	ACTIVE	1
2	2026-01-15	EM-00001	ELECTRICITY	ACTIVE	1
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, created_at, email_sent, event_type, message, reference_id, customer_id) FROM stdin;
1	2026-06-05 13:50:43.354757	f	BILL_GENERATED	Dear Constance Munyaneza, Your 6/2026 WATER utility bill of 28910.00 FRW has been successfully processed.	1	1
2	2026-06-05 13:51:27.214773	f	BILL_GENERATED	Dear Constance Munyaneza, Your 6/2026 ELECTRICITY utility bill of 21948.00 FRW has been successfully processed.	2	1
\.


--
-- Data for Name: otp_verifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.otp_verifications (id, created_at, email, expires_at, otp_hash, purpose, used) FROM stdin;
3	2026-06-05 12:30:57.486968	constancemunyaneza@gmail.com	2026-06-05 12:40:57.789686	$2a$10$IoJ6pP9qdK0Yl35/NLUDc.kn1pgeWV0EWEDtbz7OoUZq1MoM16cqq	REGISTRATION	t
6	2026-06-05 13:12:04.429804	constancemunyaneza@gmail.com	2026-06-05 13:22:04.607525	$2a$10$TVXxBDPsjcVtuCxZEPPzCuZa99w9MFbyBJkdoIjtt863ALP.p/iDi	PASSWORD_RESET	t
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.payments (id, amount_paid, payment_date, payment_method, bill_id) FROM stdin;
1	28910.00	2026-06-05	MOMO	1
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.roles (id, name) FROM stdin;
1	ROLE_ADMIN
2	ROLE_OPERATOR
3	ROLE_FINANCE
4	ROLE_CUSTOMER
\.


--
-- Data for Name: tariff_plans; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariff_plans (id, effective_from, effective_to, fixed_service_charge, flat_rate_per_unit, late_penalty_rate, meter_type, tariff_type, vat_rate, version_no) FROM stdin;
1	2026-06-05	\N	2000.00	500.00	5.0000	WATER	FLAT	18.0000	1
2	2026-06-05	\N	3000.00	\N	5.0000	ELECTRICITY	TIERED	18.0000	1
\.


--
-- Data for Name: tariff_tiers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tariff_tiers (id, max_unit, min_unit, rate_per_unit, tariff_plan_id) FROM stdin;
1	100.00	0.00	120.00	2
2	500.00	100.00	180.00	2
\.


--
-- Data for Name: user_roles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_roles (user_id, role_id) FROM stdin;
1	1
2	4
3	2
4	3
\.


--
-- Name: app_users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.app_users_id_seq', 4, true);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.audit_logs_id_seq', 19, true);


--
-- Name: bills_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.bills_id_seq', 2, true);


--
-- Name: customers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.customers_id_seq', 1, true);


--
-- Name: meter_readings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.meter_readings_id_seq', 6, true);


--
-- Name: meters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.meters_id_seq', 2, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notifications_id_seq', 2, true);


--
-- Name: otp_verifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.otp_verifications_id_seq', 6, true);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.payments_id_seq', 1, true);


--
-- Name: roles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.roles_id_seq', 4, true);


--
-- Name: tariff_plans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tariff_plans_id_seq', 2, true);


--
-- Name: tariff_tiers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tariff_tiers_id_seq', 2, true);


--
-- Name: app_users app_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: meter_readings meter_readings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT meter_readings_pkey PRIMARY KEY (id);


--
-- Name: meters meters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: otp_verifications otp_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.otp_verifications
    ADD CONSTRAINT otp_verifications_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: tariff_plans tariff_plans_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_plans
    ADD CONSTRAINT tariff_plans_pkey PRIMARY KEY (id);


--
-- Name: tariff_tiers tariff_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT tariff_tiers_pkey PRIMARY KEY (id);


--
-- Name: bills uk2dw0v70ntv7lky5j1a93q4j5s; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT uk2dw0v70ntv7lky5j1a93q4j5s UNIQUE (bill_reference);


--
-- Name: app_users uk4vj92ux8a2eehds1mdvmks473; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk4vj92ux8a2eehds1mdvmks473 UNIQUE (email);


--
-- Name: app_users uk6yycyclluv5wl4wso5946fqxg; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT uk6yycyclluv5wl4wso5946fqxg UNIQUE (national_id);


--
-- Name: bills uk_bill_meter_period; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT uk_bill_meter_period UNIQUE (meter_id, billing_month, billing_year);


--
-- Name: meter_readings uk_meter_month_year; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT uk_meter_month_year UNIQUE (meter_id, billing_month, billing_year);


--
-- Name: notifications uk_notification_event; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT uk_notification_event UNIQUE (customer_id, event_type, reference_id);


--
-- Name: customers uke9mc7sqi5m0vi278e2h2tmioe; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uke9mc7sqi5m0vi278e2h2tmioe UNIQUE (national_id);


--
-- Name: meters ukk56j5m520o5me94hml3y3u772; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT ukk56j5m520o5me94hml3y3u772 UNIQUE (meter_number);


--
-- Name: customers ukm3iom37efaxd5eucmxjqqcbe9; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT ukm3iom37efaxd5eucmxjqqcbe9 UNIQUE (phone);


--
-- Name: app_users ukmx8l8t4b18guil7nffximvl4n; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT ukmx8l8t4b18guil7nffximvl4n UNIQUE (phone_number);


--
-- Name: roles ukofx66keruapi6vyqpv6f2or37; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT ukofx66keruapi6vyqpv6f2or37 UNIQUE (name);


--
-- Name: customers ukrfbvkrffamfql7cjmen8v976v; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT ukrfbvkrffamfql7cjmen8v976v UNIQUE (email);


--
-- Name: user_roles user_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: bills trg_bill_insert_notification; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_bill_insert_notification AFTER INSERT ON public.bills FOR EACH ROW EXECUTE FUNCTION public.fn_notify_on_bill_insert();


--
-- Name: payments trg_payment_full_notification; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_payment_full_notification AFTER INSERT ON public.payments FOR EACH ROW EXECUTE FUNCTION public.fn_handle_full_payment();


--
-- Name: tariff_tiers fk2wak1449tprlhd9tyigk5nncd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT fk2wak1449tprlhd9tyigk5nncd FOREIGN KEY (tariff_plan_id) REFERENCES public.tariff_plans(id);


--
-- Name: notifications fk30dp6ycner3dgso3scgc9vghy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk30dp6ycner3dgso3scgc9vghy FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: payments fk9565r6579khpdjxnyla0l2ycd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk9565r6579khpdjxnyla0l2ycd FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: user_roles fkaf154i5th4vvgbahf8b8pa688; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkaf154i5th4vvgbahf8b8pa688 FOREIGN KEY (user_id) REFERENCES public.app_users(id);


--
-- Name: meters fkdgg79dhtsr0eumbce7ipw58lj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT fkdgg79dhtsr0eumbce7ipw58lj FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: bills fkfes5685l6y4urtsc0cq3cobo1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkfes5685l6y4urtsc0cq3cobo1 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: user_roles fkh8ciramu9cc9q3qcqiv4ue8a6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkh8ciramu9cc9q3qcqiv4ue8a6 FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- Name: bills fkjktiv3utpgao93xx3homxrhuf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkjktiv3utpgao93xx3homxrhuf FOREIGN KEY (meter_reading_id) REFERENCES public.meter_readings(id);


--
-- Name: app_users fkkgjefvkgawsrub8vxuqejkgpm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT fkkgjefvkgawsrub8vxuqejkgpm FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: meter_readings fknalaulqjlf29g1dlukdeyg0g4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fknalaulqjlf29g1dlukdeyg0g4 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fkoy9sc2dmxj2qwjeiiilf3yuxp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkoy9sc2dmxj2qwjeiiilf3yuxp FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: bills fks61p80oy2x4hir8errgydt5hk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fks61p80oy2x4hir8errgydt5hk FOREIGN KEY (approved_by) REFERENCES public.app_users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict mw8opRKAwgHxXApziswBklIRcuuj1xGc00KoFnfRSrGopWYXKztKduhHonr2IaN

