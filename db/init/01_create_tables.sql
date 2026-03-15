-- ============================================================
--  Studio Student Management — Table Definitions
--  Executed once by PostgreSQL on first container startup.
--  Every statement uses IF NOT EXISTS so re-runs are safe.
-- ============================================================

SET search_path TO studio;

-- ──────────────────────────────────────────────────────────────
--  1. students
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS studio.students (
    id              BIGSERIAL       PRIMARY KEY,
    first_name      VARCHAR(255)    NOT NULL,
    last_name       VARCHAR(255)    NOT NULL,
    phone_number    VARCHAR(255),
    pricing_type    VARCHAR(50)     NOT NULL,              -- PER_CLASS | PACKAGE
    price_per_class NUMERIC(10, 2),
    currency        VARCHAR(50),                           -- DOLLARS | EUROS | RUBLES
    timezone        VARCHAR(50)     NOT NULL,              -- StudioTimezone enum
    notes           TEXT,
    debtor          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE
);

-- ──────────────────────────────────────────────────────────────
--  2. weekly_schedules
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS studio.weekly_schedules (
    id                       BIGSERIAL       PRIMARY KEY,
    student_id               BIGINT          NOT NULL REFERENCES studio.students(id),
    day_of_week              VARCHAR(20)     NOT NULL,     -- MONDAY … SUNDAY
    start_time               TIME            NOT NULL,
    duration_minutes         INTEGER         NOT NULL,
    effective_from_epoch_day BIGINT,
    created_at               TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted                  BOOLEAN         NOT NULL DEFAULT FALSE
);

-- ──────────────────────────────────────────────────────────────
--  3. package_purchases
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS studio.package_purchases (
    id                BIGSERIAL       PRIMARY KEY,
    student_id        BIGINT          NOT NULL REFERENCES studio.students(id),
    total_classes     INTEGER         NOT NULL,
    classes_remaining INTEGER         NOT NULL,
    amount_paid       NUMERIC(10, 2)  NOT NULL,
    payment_date      DATE            NOT NULL,
    description       VARCHAR(255),
    created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN         NOT NULL DEFAULT FALSE
);

-- ──────────────────────────────────────────────────────────────
--  4. class_sessions
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS studio.class_sessions (
    id                  BIGSERIAL       PRIMARY KEY,
    student_id          BIGINT          NOT NULL REFERENCES studio.students(id),
    weekly_schedule_id  BIGINT          REFERENCES studio.weekly_schedules(id),
    class_date          DATE            NOT NULL,
    start_time          TIME            NOT NULL,
    duration_minutes    INTEGER         NOT NULL,
    status              VARCHAR(50)     NOT NULL DEFAULT 'SCHEDULED',  -- SCHEDULED | COMPLETED | CANCELLED | MOVED
    payment_status      VARCHAR(50)     NOT NULL DEFAULT 'UNPAID',     -- UNPAID | PAID | PACKAGE | REFUNDED
    price_charged       NUMERIC(10, 2),
    package_purchase_id BIGINT          REFERENCES studio.package_purchases(id),
    is_one_off          BOOLEAN         NOT NULL DEFAULT FALSE,
    note                VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE
);

-- ──────────────────────────────────────────────────────────────
--  5. payers
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS studio.payers (
    id           BIGSERIAL       PRIMARY KEY,
    student_id   BIGINT          NOT NULL REFERENCES studio.students(id),
    full_name    VARCHAR(255)    NOT NULL,
    phone_number VARCHAR(255),
    note         VARCHAR(255),
    created_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN         NOT NULL DEFAULT FALSE
);

