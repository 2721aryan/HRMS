-- V1: Core foundation — roles, org structure, employees, users, audit
-- Conventions:
--   * BIGINT GENERATED ALWAYS AS IDENTITY primary keys
--   * TIMESTAMPTZ (stored in UTC) for all timestamps
--   * VARCHAR + CHECK instead of PG enums (easier to extend in later migrations)
--   * Soft delete via is_active / status — never DELETE employee rows
--   * created_by / updated_by are BIGINT references to users.id (no FK to avoid circular deps)

-- ---------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,   -- ADMIN, HR, MANAGER, EMPLOYEE
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255)
);

-- ---------------------------------------------------------------
-- Org structure
-- ---------------------------------------------------------------
CREATE TABLE locations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    address     VARCHAR(255),
    city        VARCHAR(80),
    state       VARCHAR(80),
    country     VARCHAR(80)  NOT NULL DEFAULT 'India',
    timezone    VARCHAR(50)  NOT NULL DEFAULT 'Asia/Kolkata',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE departments (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code             VARCHAR(20)  NOT NULL UNIQUE,
    name             VARCHAR(100) NOT NULL UNIQUE,
    parent_id        BIGINT REFERENCES departments(id),  -- supports sub-departments
    head_employee_id BIGINT,                             -- FK added after employees table
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       BIGINT,
    updated_by       BIGINT
);

CREATE TABLE designations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(100) NOT NULL UNIQUE,
    grade       VARCHAR(20),                 -- L1, L2, M1 … used by payroll in Phase 2
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT
);

-- ---------------------------------------------------------------
-- Employees
-- ---------------------------------------------------------------
CREATE TABLE employees (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_code           VARCHAR(20)  NOT NULL UNIQUE,   -- e.g. EMP-0001
    first_name              VARCHAR(80)  NOT NULL,
    last_name               VARCHAR(80),
    work_email              VARCHAR(150) NOT NULL UNIQUE,
    personal_email          VARCHAR(150),
    phone                   VARCHAR(20),
    date_of_birth           DATE,
    gender                  VARCHAR(20)  CHECK (gender IN ('MALE','FEMALE','OTHER','UNDISCLOSED')),
    marital_status          VARCHAR(20),
    address_line            VARCHAR(255),
    city                    VARCHAR(80),
    state                   VARCHAR(80),
    postal_code             VARCHAR(12),
    emergency_contact_name  VARCHAR(100),
    emergency_contact_phone VARCHAR(20),

    -- Employment details
    department_id      BIGINT REFERENCES departments(id),
    designation_id     BIGINT REFERENCES designations(id),
    location_id        BIGINT REFERENCES locations(id),
    manager_id         BIGINT REFERENCES employees(id),    -- reporting manager
    employment_type    VARCHAR(20)  NOT NULL DEFAULT 'FULL_TIME'
                       CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERN')),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                       CHECK (status IN ('ACTIVE','ON_NOTICE','INACTIVE')),
    date_of_joining    DATE         NOT NULL,
    probation_end_date DATE,
    date_of_exit       DATE,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         BIGINT,
    updated_by         BIGINT,

    CONSTRAINT chk_no_self_manager CHECK (manager_id IS NULL OR manager_id <> id),
    CONSTRAINT chk_exit_after_join CHECK (date_of_exit IS NULL OR date_of_exit >= date_of_joining)
);

-- Resolve the circular FK: departments.head_employee_id -> employees
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head FOREIGN KEY (head_employee_id) REFERENCES employees(id);

CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_manager    ON employees(manager_id);
CREATE INDEX idx_employees_status     ON employees(status);
CREATE INDEX idx_employees_name       ON employees(lower(first_name), lower(last_name));

-- File metadata only; actual files live in local disk (dev) or S3/MinIO (prod)
-- is_active: mark old versions FALSE when a document is replaced; never hard-delete
CREATE TABLE employee_documents (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id  BIGINT       NOT NULL REFERENCES employees(id),
    doc_type     VARCHAR(40)  NOT NULL,        -- ID_PROOF, OFFER_LETTER, CONTRACT, LEAVE_ATTACHMENT, PROFILE_PHOTO …
    file_name    VARCHAR(255) NOT NULL,
    storage_key  VARCHAR(500) NOT NULL,        -- relative path or S3 object key
    content_type VARCHAR(100),
    size_bytes   BIGINT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    uploaded_by  BIGINT,                       -- users.id (no FK — avoids circular dep)
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_employee_documents_emp ON employee_documents(employee_id);

-- ---------------------------------------------------------------
-- Users / authentication
-- Kept separate from employees:
--   * Not every employee needs a login
--   * Auth concerns stay isolated from HR data
-- ---------------------------------------------------------------
CREATE TABLE users (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id          BIGINT       NOT NULL UNIQUE REFERENCES employees(id),
    username             VARCHAR(150) NOT NULL UNIQUE,    -- typically work_email
    password_hash        VARCHAR(255) NOT NULL,           -- BCrypt (60 chars); VARCHAR(255) gives room for future algorithms
    token_version        INT          NOT NULL DEFAULT 0, -- increment to instantly invalidate ALL tokens for this user
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_attempts      INT          NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens: store only the hash, never the raw token
CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,   -- SHA-256 hex of the raw token
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,                    -- set on logout; access token expires naturally (~15 min)
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ---------------------------------------------------------------
-- Audit log (generic — every module writes here)
-- ---------------------------------------------------------------
CREATE TABLE audit_logs (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type  VARCHAR(60)  NOT NULL,       -- 'EMPLOYEE', 'LEAVE_REQUEST', 'PAYROLL_RUN' …
    entity_id    BIGINT       NOT NULL,
    action       VARCHAR(20)  NOT NULL,       -- CREATE, UPDATE, DELETE, APPROVE, REJECT …
    old_values   JSONB,
    new_values   JSONB,
    performed_by BIGINT,                      -- users.id (nullable: system actions have no user)
    performed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ip_address   VARCHAR(45)                  -- supports IPv6
);
CREATE INDEX idx_audit_entity       ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_time         ON audit_logs(performed_at);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);  -- "what did this admin do?"
