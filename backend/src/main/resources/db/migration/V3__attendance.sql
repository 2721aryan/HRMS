-- V3: Attendance
-- Depends on: V1 (employees), V2 (leave_requests)
-- NOTE: This migration is written now but applied when the attendance build begins.
--       Flyway applies in version order; do not apply V3 until the attendance module is ready.

-- btree_gist is required for the EXCLUDE constraint on employee_shifts.
-- Available on virtually all managed PostgreSQL services (RDS, Cloud SQL, Supabase, etc.).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ---------------------------------------------------------------
-- Shifts
-- ---------------------------------------------------------------
-- weekly_offs stored as 7 boolean columns (not a comma-separated string).
-- This is queryable, type-safe, and requires no parsing in application code.
-- Current policy: Saturday + Sunday off (5-day week, single location).
-- If alternate-Saturday policy is needed in the future, migrate to a
-- shift_weekly_offs child table with (shift_id, day_of_week, week_numbers[]).
CREATE TABLE shifts (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name             VARCHAR(60)  NOT NULL UNIQUE,
    start_time       TIME         NOT NULL,
    end_time         TIME         NOT NULL,
    grace_minutes    INT          NOT NULL DEFAULT 15,   -- minutes after start_time before marked late
    full_day_minutes INT          NOT NULL DEFAULT 480,  -- minutes worked = full day
    half_day_minutes INT          NOT NULL DEFAULT 240,  -- minutes worked = half day
    off_sunday       BOOLEAN      NOT NULL DEFAULT TRUE,
    off_monday       BOOLEAN      NOT NULL DEFAULT FALSE,
    off_tuesday      BOOLEAN      NOT NULL DEFAULT FALSE,
    off_wednesday    BOOLEAN      NOT NULL DEFAULT FALSE,
    off_thursday     BOOLEAN      NOT NULL DEFAULT FALSE,
    off_friday       BOOLEAN      NOT NULL DEFAULT FALSE,
    off_saturday     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------
-- Employee shift assignments (effective-dated history)
-- EXCLUDE constraint prevents overlapping shift assignments for the same employee.
-- This is enforced at the database level, not just the service layer.
-- ---------------------------------------------------------------
CREATE TABLE employee_shifts (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id    BIGINT NOT NULL REFERENCES employees(id),
    shift_id       BIGINT NOT NULL REFERENCES shifts(id),
    effective_from DATE   NOT NULL,
    effective_to   DATE,                 -- NULL = current / open-ended assignment
    CONSTRAINT chk_shift_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    EXCLUDE USING gist (
        employee_id WITH =,
        daterange(effective_from, COALESCE(effective_to, '9999-12-31'), '[]') WITH &&
    )
);
CREATE INDEX idx_employee_shifts_emp ON employee_shifts(employee_id, effective_from);

-- ---------------------------------------------------------------
-- Attendance records — one summary row per employee per calendar day
-- ---------------------------------------------------------------
CREATE TABLE attendance_records (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id     BIGINT       NOT NULL REFERENCES employees(id),
    attendance_date DATE         NOT NULL,
    check_in        TIMESTAMPTZ,
    check_out       TIMESTAMPTZ,
    work_minutes    INT          NOT NULL DEFAULT 0,
    status          VARCHAR(15)  NOT NULL
                    CHECK (status IN ('PRESENT','ABSENT','HALF_DAY','ON_LEAVE','HOLIDAY','WEEKLY_OFF')),
    is_late         BOOLEAN      NOT NULL DEFAULT FALSE,
    source          VARCHAR(15)  NOT NULL DEFAULT 'WEB'
                    CHECK (source IN ('WEB','MANUAL','BIOMETRIC','IMPORT','SYSTEM')),
    leave_request_id BIGINT REFERENCES leave_requests(id),   -- attendance -> leave (leave stays independent)
    -- locked: TRUE once the payroll run for this month is processed.
    -- Revisit in Phase 2: consider a period_locks table (one row per month) to also cover leave.
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    remarks         VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    UNIQUE (employee_id, attendance_date),
    CONSTRAINT chk_check_times CHECK (check_out IS NULL OR check_in IS NULL OR check_out >= check_in)
);
CREATE INDEX idx_attendance_date ON attendance_records(attendance_date);

-- ---------------------------------------------------------------
-- Raw punches — needed for biometric integration (Phase 2)
-- Schema is written now so the table is ready; application code not wired in Phase 1.
-- punch_type NULL is intentional: some biometric devices do not distinguish IN from OUT.
-- ---------------------------------------------------------------
CREATE TABLE attendance_punches (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id BIGINT       NOT NULL REFERENCES employees(id),
    punch_time  TIMESTAMPTZ  NOT NULL,
    punch_type  VARCHAR(5)   CHECK (punch_type IN ('IN','OUT')),   -- NULL = device does not differentiate
    source      VARCHAR(15)  NOT NULL DEFAULT 'WEB'
                CHECK (source IN ('WEB','BIOMETRIC','IMPORT')),
    device_id   VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_punches_emp_time ON attendance_punches(employee_id, punch_time);

-- ---------------------------------------------------------------
-- Attendance regularizations — "I forgot to punch / made an error"
-- ---------------------------------------------------------------
CREATE TABLE attendance_regularizations (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          BIGINT       NOT NULL REFERENCES employees(id),
    attendance_date      DATE         NOT NULL,
    -- Nullable FK: the attendance_record may not exist yet when the request is submitted
    -- (e.g., missed punch on a day that was never recorded).
    -- Populate when the request is created (if record exists) or on approval.
    attendance_record_id BIGINT REFERENCES attendance_records(id),
    requested_check_in   TIMESTAMPTZ,
    requested_check_out  TIMESTAMPTZ,
    reason               VARCHAR(500) NOT NULL,
    status               VARCHAR(15)  NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    approver_id          BIGINT REFERENCES employees(id),
    decided_at           TIMESTAMPTZ,
    decision_comment     VARCHAR(500),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_regularization_emp  ON attendance_regularizations(employee_id, attendance_date);
CREATE INDEX idx_regularization_appr ON attendance_regularizations(approver_id, status);
