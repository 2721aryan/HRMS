-- V4: Seed reference data
-- Depends on: V1 (roles), V2 (leave_types), V3 (shifts)
-- Safe to re-run on every environment (uses INSERT … ON CONFLICT DO NOTHING).
--
-- IMPORTANT: Do NOT seed an ADMIN user with a hardcoded password here.
-- Create the first admin account in a Spring Boot ApplicationRunner
-- using credentials from environment variables (ADMIN_USERNAME, ADMIN_PASSWORD).

-- ---------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------
INSERT INTO roles (code, name, description) VALUES
    ('ADMIN',    'System Admin', 'Full system access and configuration'),
    ('HR',       'HR Manager',   'Manage employees, leave, attendance, reports'),
    ('MANAGER',  'Manager',      'View team, approve team leave and regularizations'),
    ('EMPLOYEE', 'Employee',     'Self-service: profile, leave requests, attendance')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------
-- Leave types (default policy — client will adjust quotas and rules)
-- ---------------------------------------------------------------
INSERT INTO leave_types (
    code, name, is_paid, annual_quota, accrual_type,
    carry_forward_allowed, max_carry_forward,
    allow_half_day, requires_document, applicable_gender
) VALUES
    ('CL',  'Casual Leave',    TRUE,  12,  'YEARLY',  FALSE, 0,  TRUE,  FALSE, NULL),
    ('SL',  'Sick Leave',      TRUE,  8,   'YEARLY',  FALSE, 0,  FALSE, FALSE, NULL),
    ('PL',  'Privilege Leave', TRUE,  15,  'MONTHLY', TRUE,  30, TRUE,  FALSE, NULL),
    ('LOP', 'Loss of Pay',     FALSE, 0,   'NONE',    FALSE, 0,  TRUE,  FALSE, NULL),
    ('MAT', 'Maternity Leave', TRUE,  182, 'NONE',    FALSE, 0,  FALSE, TRUE,  'FEMALE'),
    ('PAT', 'Paternity Leave', TRUE,  5,   'NONE',    FALSE, 0,  FALSE, FALSE, 'MALE')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------
-- Default shift (5-day week: Mon–Fri; Sat + Sun off)
-- off_saturday and off_sunday default to TRUE in the schema.
-- ---------------------------------------------------------------
INSERT INTO shifts (name, start_time, end_time, grace_minutes, full_day_minutes, half_day_minutes)
VALUES ('General Shift', '09:30', '18:30', 15, 480, 240)
ON CONFLICT (name) DO NOTHING;
