-- V2: Leave management
-- Depends on: V1 (employees, locations, employee_documents)

-- ---------------------------------------------------------------
-- Leave types (configurable per company policy)
-- ---------------------------------------------------------------
CREATE TABLE leave_types (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                  VARCHAR(20)  NOT NULL UNIQUE,   -- CL, SL, PL, LOP, MAT, PAT
    name                  VARCHAR(80)  NOT NULL,
    is_paid               BOOLEAN      NOT NULL DEFAULT TRUE,
    annual_quota          NUMERIC(5,1) NOT NULL DEFAULT 0,
    accrual_type          VARCHAR(20)  NOT NULL DEFAULT 'YEARLY'
                          CHECK (accrual_type IN ('YEARLY','MONTHLY','NONE')),
    carry_forward_allowed BOOLEAN      NOT NULL DEFAULT FALSE,
    max_carry_forward     NUMERIC(5,1) NOT NULL DEFAULT 0,
    allow_half_day        BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_document     BOOLEAN      NOT NULL DEFAULT FALSE,
    max_consecutive_days  NUMERIC(5,1),                   -- NULL = no cap
    applicable_gender     VARCHAR(20),                    -- NULL = all; 'FEMALE' = MAT, 'MALE' = PAT
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            BIGINT,
    updated_by            BIGINT
);

-- ---------------------------------------------------------------
-- Holidays
-- ---------------------------------------------------------------
CREATE TABLE holidays (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    holiday_date DATE         NOT NULL,
    name         VARCHAR(120) NOT NULL,
    location_id  BIGINT REFERENCES locations(id),   -- NULL = applies to all locations
    is_optional  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   BIGINT
);
-- COALESCE(location_id, 0) makes the unique index work when location_id is NULL
-- (two NULLs are not equal in SQL, so a plain UNIQUE would allow duplicates)
CREATE UNIQUE INDEX uq_holidays_date_location
    ON holidays (holiday_date, COALESCE(location_id, 0));

-- ---------------------------------------------------------------
-- Leave balances — one row per employee / leave_type / year
-- ---------------------------------------------------------------
CREATE TABLE leave_balances (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id   BIGINT       NOT NULL REFERENCES employees(id),
    leave_type_id BIGINT       NOT NULL REFERENCES leave_types(id),
    year          SMALLINT     NOT NULL,
    opening       NUMERIC(5,1) NOT NULL DEFAULT 0,   -- carried forward from previous year
    accrued       NUMERIC(5,1) NOT NULL DEFAULT 0,
    adjusted      NUMERIC(5,1) NOT NULL DEFAULT 0,   -- manual HR adjustments (positive or negative)
    used          NUMERIC(5,1) NOT NULL DEFAULT 0,
    pending       NUMERIC(5,1) NOT NULL DEFAULT 0,   -- requested but not yet approved
    -- NOTE: available CAN be negative. This is intentional for LOP (Loss of Pay).
    -- LOP requests must SKIP the balance check in the service layer.
    -- All other leave types must validate available > 0 before creating a leave_request.
    available     NUMERIC(5,1) GENERATED ALWAYS AS
                  (opening + accrued + adjusted - used - pending) STORED,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (employee_id, leave_type_id, year)
);

-- ---------------------------------------------------------------
-- Leave requests
-- ---------------------------------------------------------------
CREATE TABLE leave_requests (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id      BIGINT       NOT NULL REFERENCES employees(id),
    leave_type_id    BIGINT       NOT NULL REFERENCES leave_types(id),
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    start_day_part   VARCHAR(15)  NOT NULL DEFAULT 'FULL_DAY'
                     CHECK (start_day_part IN ('FULL_DAY','FIRST_HALF','SECOND_HALF')),
    end_day_part     VARCHAR(15)  NOT NULL DEFAULT 'FULL_DAY'
                     CHECK (end_day_part IN ('FULL_DAY','FIRST_HALF','SECOND_HALF')),
    total_days       NUMERIC(4,1) NOT NULL CHECK (total_days > 0),   -- excludes weekends & holidays
    reason           VARCHAR(500),
    status           VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    approver_id      BIGINT REFERENCES employees(id),
    decided_at       TIMESTAMPTZ,
    decision_comment VARCHAR(500),
    document_id      BIGINT REFERENCES employee_documents(id),       -- supporting doc (e.g. medical cert)
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       BIGINT,
    updated_by       BIGINT,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_leave_requests_emp    ON leave_requests(employee_id, start_date);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_appr   ON leave_requests(approver_id, status);

-- ---------------------------------------------------------------
-- Leave balance ledger
-- Every change to a balance is recorded here (immutable audit trail).
-- Reconstruct any balance at any point in time by replaying this table.
-- days is signed: positive = credit, negative = debit.
-- ---------------------------------------------------------------
CREATE TABLE leave_balance_transactions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    leave_balance_id BIGINT       NOT NULL REFERENCES leave_balances(id),
    txn_type         VARCHAR(20)  NOT NULL
                     CHECK (txn_type IN (
                         'ACCRUAL',          -- monthly/yearly credit
                         'CARRY_FORWARD',    -- opening balance from prior year
                         'REQUEST',          -- deducted when request is submitted (pending)
                         'APPROVAL',         -- confirmed deduction on approval
                         'REJECTION',        -- reversal when request is rejected
                         'CANCELLATION',     -- reversal when employee cancels
                         'ADJUSTMENT'        -- manual HR adjustment
                     )),
    days             NUMERIC(5,1) NOT NULL,          -- signed: positive = credit, negative = debit
    leave_request_id BIGINT REFERENCES leave_requests(id),
    remarks          VARCHAR(255),
    created_by       BIGINT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_lbt_balance ON leave_balance_transactions(leave_balance_id);
