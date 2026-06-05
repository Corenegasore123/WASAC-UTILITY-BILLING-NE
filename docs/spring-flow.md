flowchart TB

%% =========================
%% SYSTEM USERS
%% =========================
subgraph Actors
ADMIN[Admin]
OPERATOR[Operator]
FINANCE[Finance Officer]
CUSTOMER[Customer]
end

%% =========================
%% AUTHENTICATION
%% =========================
subgraph Authentication & Security
LOGIN[Login Request]
AUTH[Spring Security]
JWT[JWT Token Provider]
CHANGEPWD[Force Password Change]
end

ADMIN --> LOGIN
OPERATOR --> LOGIN
FINANCE --> LOGIN
CUSTOMER --> LOGIN

LOGIN --> AUTH
AUTH --> JWT
JWT --> CHANGEPWD

%% =========================
%% USER MANAGEMENT
%% =========================
subgraph User Management
STAFF[Create Staff Account]
ROLE[Assign Role]
TEMP[Generate Temporary Password]

    PROFILE[Create Customer Profile]
    LINK[Link Login Account]

    ACTIVE[Active User Account]
end

ADMIN --> STAFF
STAFF --> ROLE
ROLE --> TEMP
TEMP --> ACTIVE

ADMIN --> PROFILE
OPERATOR --> PROFILE
PROFILE --> LINK
LINK --> ACTIVE

CUSTOMER --> ACTIVE

%% =========================
%% METER READING
%% =========================
subgraph Meter Reading Management
READING[Record Meter Reading]
METERCHECK{Meter Active?}
VALIDATE{Reading > Previous?}
SAVE[Save Reading]
end

OPERATOR --> READING
READING --> METERCHECK

METERCHECK -->|Yes| VALIDATE
METERCHECK -->|No| REJECT1[Reject Reading]

VALIDATE -->|Yes| SAVE
VALIDATE -->|No| REJECT1

%% =========================
%% BILLING PROCESS
%% =========================
subgraph Billing Management
BILLGEN[Generate Bill]
CUSTCHECK{Customer Active?}
TARIFF[Resolve Tariff]
CALC[Calculate Usage VAT & Penalties]

    PENDING[Bill Status:<br/>PENDING_APPROVAL]
    APPROVE[Approve Bill]
    APPROVED[Bill Status:<br/>APPROVED]
end

SAVE --> BILLGEN
BILLGEN --> CUSTCHECK

CUSTCHECK -->|Yes| TARIFF
CUSTCHECK -->|No| REJECT2[Cancel Billing]

TARIFF --> CALC
CALC --> PENDING

FINANCE --> APPROVE
PENDING --> APPROVE
APPROVE --> APPROVED

%% =========================
%% PAYMENT PROCESS
%% =========================
subgraph Payment Management
PAYMENT[Record Payment]
BALANCE[Update Outstanding Balance]
FULL{Balance = 0?}
PAID[Status: PAID]
PARTIAL[Status: PARTIALLY_PAID]
end

CUSTOMER --> PAYMENT
FINANCE --> PAYMENT

APPROVED --> PAYMENT
PAYMENT --> BALANCE
BALANCE --> FULL

FULL -->|Yes| PAID
FULL -->|No| PARTIAL

%% =========================
%% NOTIFICATIONS
%% =========================
subgraph Notifications
BILLNOTIF[Bill Notification]
PAYNOTIF[Payment Notification]
end

PENDING --> BILLNOTIF
PAID --> PAYNOTIF

%% =========================
%% APPLICATION ARCHITECTURE
%% =========================
subgraph System Architecture
CONTROLLERS[REST Controllers]
SERVICES[Business Services]
SECURITY[Spring Security + JWT]
REPOSITORIES[JPA Repositories]
DATABASE[(PostgreSQL)]
FLYWAY[Flyway Migrations]
end

CONTROLLERS --> SERVICES
SERVICES --> REPOSITORIES
REPOSITORIES --> DATABASE

SECURITY --> CONTROLLERS
FLYWAY --> DATABASE

AUTH -.uses.-> SECURITY
LOGIN -.accesses.-> CONTROLLERS
STAFF -.uses.-> CONTROLLERS
PROFILE -.uses.-> CONTROLLERS
READING -.uses.-> CONTROLLERS
PAYMENT -.uses.-> CONTROLLERS