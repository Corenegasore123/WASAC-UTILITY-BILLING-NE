// WASAC & REG Utility Billing System
// Entity Relationship Diagram
// Database: PostgreSQL

// ─────────────────────────────────────────
// USERS TABLE
// Stores all system users: Admin, Operator, Finance, Customer
// ─────────────────────────────────────────
Table users {
id          bigserial     [pk, increment]
full_name   varchar(100)  [not null]
email       varchar(100)  [unique, not null]
phone_number varchar(15)  [unique, not null]
password    varchar(255)  [not null]
role        varchar(20)   [not null, note: 'ADMIN | OPERATOR | FINANCE | CUSTOMER']
status      varchar(20)   [not null, default: 'ACTIVE', note: 'ACTIVE | INACTIVE']
first_login boolean       [not null, default: true, note: 'true = must change password']
created_at  timestamp     [default: `now()`]
}

// ─────────────────────────────────────────
// CUSTOMERS TABLE
// Stores utility customers (water/electricity consumers)
// ─────────────────────────────────────────
Table customers {
id            bigserial    [pk, increment]
full_name     varchar(100) [not null]
national_id   varchar(20)  [unique, not null, note: 'Rwanda National ID - must be unique']
email         varchar(100) [unique, not null]
phone_number  varchar(15)  [unique, not null, note: 'Rwanda format: 07[2389]XXXXXXX']
address       varchar(255) [not null]
date_of_birth date         [note: 'Must be >= 18 years if provided']
status        varchar(20)  [not null, default: 'ACTIVE', note: 'ACTIVE | INACTIVE | SUSPENDED']
created_at    timestamp    [default: `now()`]
}

// ─────────────────────────────────────────
// METERS TABLE
// Each customer can have one or more meters
// ─────────────────────────────────────────
Table meters {
id                bigserial   [pk, increment]
customer_id       bigint      [not null, ref: > customers.id]
meter_number      varchar(20) [unique, not null, note: 'eg WM-10001 or EM-10001']
meter_type        varchar(15) [not null, note: 'WATER | ELECTRICITY']
installation_date date        [not null]
status            varchar(20) [not null, default: 'ACTIVE', note: 'ACTIVE | INACTIVE | DISCONNECTED']
}

// ─────────────────────────────────────────
// METER READINGS TABLE
// Monthly readings captured by operators
// Unique per meter per month per year
// ─────────────────────────────────────────
Table meter_readings {
id               bigserial [pk, increment]
meter_id         bigint    [not null, ref: > meters.id]
operator_id      bigint    [not null, ref: > users.id, note: 'Must have OPERATOR role']
previous_reading double    [not null]
current_reading  double    [not null, note: 'Must be > previous_reading']
consumption      double    [not null, note: 'current - previous, must be >= 0']
reading_date     date      [not null, note: 'Cannot be a future date']
reading_month    int       [not null]
reading_year     int       [not null]

indexes {
(meter_id, reading_month, reading_year) [unique, name: 'uq_reading_per_meter_month']
}
}

// ─────────────────────────────────────────
// TARIFFS TABLE
// Configured by Admin - versioned pricing rules
// New tariffs apply only to future billing cycles
// ─────────────────────────────────────────
Table tariffs {
id              bigserial    [pk, increment]
meter_type      varchar(15)  [not null, note: 'WATER | ELECTRICITY']
tariff_type     varchar(10)  [not null, note: 'FLAT | TIERED']
unit_price      double       [not null, note: 'Must be > 0 for FLAT type']
service_charge  double       [not null, default: 0, note: 'Fixed monthly charge >= 0']
vat_percentage  double       [not null, default: 18, note: 'Range: 0 to 100']
penalty_rate    double       [not null, default: 0, note: 'Late payment penalty >= 0']
version         int          [not null, default: 1]
effective_date  date         [not null, note: 'Must be today or a future date']
active          boolean      [not null, default: true]
created_at      timestamp    [default: `now()`]
}

// ─────────────────────────────────────────
// TARIFF TIERS TABLE
// Used when tariff_type = TIERED
// Ranges must not overlap
// ─────────────────────────────────────────
Table tariff_tiers {
id             bigserial [pk, increment]
tariff_id      bigint    [not null, ref: > tariffs.id]
min_units      double    [not null, note: 'Start of consumption range']
max_units      double    [not null, note: 'End of consumption range']
price_per_unit double    [not null, note: 'Price for this tier, must be > 0']
}

// ─────────────────────────────────────────
// BILLS TABLE
// Generated after meter reading is captured
// One bill per meter per month per year
// ─────────────────────────────────────────
Table bills {
id                  bigserial    [pk, increment]
customer_id         bigint       [not null, ref: > customers.id]
meter_reading_id    bigint       [not null, ref: > meter_readings.id]
tariff_id           bigint       [not null, ref: > tariffs.id]
approved_by         bigint       [ref: > users.id, note: 'Finance officer who approved']
bill_reference      varchar(50)  [unique, not null, note: 'Auto-generated unique ref']
units_consumed      double       [not null]
tariff_amount       double       [not null]
service_charge      double       [not null]
vat_amount          double       [not null]
penalty_amount      double       [not null, default: 0]
total_amount        double       [not null, note: 'Must be >= 0']
outstanding_balance double       [not null]
meter_type          varchar(15)  [not null, note: 'WATER | ELECTRICITY']
status              varchar(20)  [not null, default: 'UNPAID', note: 'UNPAID | PARTIALLY_PAID | PAID | APPROVED']
billing_month       int          [not null]
billing_year        int          [not null]
due_date            date         [not null]
approved            boolean      [not null, default: false]
created_at          timestamp    [default: `now()`]

indexes {
(meter_reading_id, billing_month, billing_year) [unique, name: 'uq_bill_per_meter_month']
}
}

// ─────────────────────────────────────────
// PAYMENTS TABLE
// Records customer payments - supports partial and full
// ─────────────────────────────────────────
Table payments {
id             bigserial   [pk, increment]
bill_id        bigint      [not null, ref: > bills.id]
customer_id    bigint      [not null, ref: > customers.id]
bill_reference varchar(50) [not null]
amount_paid    double      [not null, note: 'Must be > 0 and <= outstanding_balance']
payment_method varchar(10) [not null, note: 'MOMO | BANK | CARD | CASH']
payment_date   date        [not null, note: 'Cannot be a future date']
balance_after  double      [not null, note: 'Remaining balance after this payment']
created_at     timestamp   [default: `now()`]
}

// ─────────────────────────────────────────
// NOTIFICATIONS TABLE
// Created by DB trigger on bill generation and full payment
// Also sent via email using JavaMailSender
// ─────────────────────────────────────────
Table notifications {
id          bigserial    [pk, increment]
customer_id bigint       [not null, ref: > customers.id]
bill_id     bigint       [not null, ref: > bills.id]
message     text         [not null, note: 'Dear [name], your [type] bill of [amount] FRW...']
type        varchar(30)  [not null, note: 'BILL_GENERATED | BILL_PAID | ROLE_ASSIGNED']
is_read     boolean      [not null, default: false]
email_sent  boolean      [not null, default: false, note: 'true after JavaMailSender fires']
created_at  timestamp    [default: `now()`]
}

// ─────────────────────────────────────────
// AUDIT LOGS TABLE
// Tracks every major action in the system
// Who did what, when, and what changed
// ─────────────────────────────────────────
Table audit_logs {
id           bigserial   [pk, increment]
user_id      bigint      [not null, ref: > users.id]
action_type  varchar(50) [not null, note: 'USER_CREATED | BILL_APPROVED | PAYMENT_MADE | TARIFF_UPDATED etc']
entity_name  varchar(50) [not null, note: 'eg: Bill, Payment, User, Meter']
entity_id    bigint      [not null]
old_value    text        [note: 'JSON snapshot before change']
new_value    text        [note: 'JSON snapshot after change']
performed_at timestamp   [default: `now()`]
}