-- ============================================================
--  Test data for integration tests
--  Loaded via @Sql before each test class
-- ============================================================

-- ── Students ────────────────────────────────────────────────
INSERT INTO studio.students (id, first_name, last_name, phone_number, pricing_type, price_per_class, currency, timezone, class_type, start_date, holiday_mode, holiday_from, holiday_to, stopped_attending, notes, debtor, created_at, updated_at, deleted)
VALUES
    (1, 'Ana',   'García',  '+34600111222', 'PER_CLASS', 30.00, 'EUROS',   'SPAIN',          'EGE',    NULL, FALSE, NULL, NULL, FALSE, 'Intermediate level', FALSE, NOW(), NOW(), FALSE),
    (2, 'Ivan',  'Petrov',  '+79001112233', 'PACKAGE',   NULL,  'RUBLES',  'RUSSIA_MOSCOW',  'OGE',    NULL, FALSE, NULL, NULL, FALSE, NULL,                 TRUE,  NOW(), NOW(), FALSE),
    (3, 'John',  'Smith',   '+12025551234', 'PER_CLASS', 40.00, 'DOLLARS', 'SPAIN',          'IELTS',  NULL, FALSE, NULL, NULL, FALSE, NULL,                 FALSE, NOW(), NOW(), FALSE),
    (4, 'Deleted','User',   NULL,           'PER_CLASS', 25.00, 'EUROS',   'SPAIN',          'CASUAL', NULL, FALSE, NULL, NULL, FALSE, NULL,                 FALSE, NOW(), NOW(), TRUE);

-- ── Weekly schedules ────────────────────────────────────────
INSERT INTO studio.weekly_schedules (id, student_id, day_of_week, start_time, duration_minutes, created_at, updated_at, deleted)
VALUES
    (1, 1, 'MONDAY',    '10:00', 60, NOW(), NOW(), FALSE),
    (2, 1, 'WEDNESDAY', '10:00', 60, NOW(), NOW(), FALSE),
    (3, 2, 'TUESDAY',   '14:00', 45, NOW(), NOW(), FALSE);

-- ── Class sessions ──────────────────────────────────────────
INSERT INTO studio.class_sessions (id, student_id, weekly_schedule_id, class_date, start_time, timezone, duration_minutes, status, payment_status, price_charged, currency, package_purchase_id, is_one_off, note, created_at, updated_at, deleted)
VALUES
    -- Ana: past paid session
    (1, 1, 1, '2026-03-02', '10:00', 'SPAIN', 60, 'COMPLETED', 'PAID',   30.00, 'EUROS',   NULL, FALSE, NULL,           NOW(), NOW(), FALSE),
    -- Ana: today's session, unpaid
    (2, 1, 2, '2026-03-12', '10:00', 'SPAIN', 60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS',   NULL, FALSE, NULL,           NOW(), NOW(), FALSE),
    -- Ana: future session
    (3, 1, 1, '2026-03-16', '10:00', 'SPAIN', 60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS',   NULL, FALSE, NULL,           NOW(), NOW(), FALSE),
    -- John: paid session on March 5
    (4, 3, NULL, '2026-03-05', '14:00', 'SPAIN', 60, 'COMPLETED', 'PAID', 40.00, 'DOLLARS', NULL, TRUE,  'Extra class',  NOW(), NOW(), FALSE),
    -- John: paid session on March 10
    (5, 3, NULL, '2026-03-10', '14:00', 'SPAIN', 60, 'COMPLETED', 'PAID', 40.00, 'DOLLARS', NULL, TRUE,  NULL,           NOW(), NOW(), FALSE);

-- ── Package purchases ───────────────────────────────────────
INSERT INTO studio.package_purchases (id, student_id, total_classes, classes_remaining, amount_paid, currency, payment_date, description, created_at, updated_at, deleted)
VALUES
    (1, 2, 10, 8, 15000.00, 'RUBLES', '2026-03-01', 'March 10-class bundle', NOW(), NOW(), FALSE),
    (2, 2,  5, 0,  8000.00, 'RUBLES', '2026-02-01', 'February bundle (exhausted)', NOW(), NOW(), FALSE);

-- ── Payers ──────────────────────────────────────────────────
INSERT INTO studio.payers (id, student_id, full_name, phone_number, note, created_at, updated_at, deleted)
VALUES
    (1, 1, 'María García', '+34600999888', 'Mother', NOW(), NOW(), FALSE),
    (2, 2, 'Olga Petrova', '+79009998877', 'Mother', NOW(), NOW(), FALSE);

-- ── Advance sequences past manually-inserted IDs ────────────
-- Prevents H2 auto-increment from colliding with seeded rows
ALTER TABLE studio.students          ALTER COLUMN id RESTART WITH 100;
ALTER TABLE studio.weekly_schedules  ALTER COLUMN id RESTART WITH 100;
ALTER TABLE studio.class_sessions    ALTER COLUMN id RESTART WITH 100;
ALTER TABLE studio.package_purchases ALTER COLUMN id RESTART WITH 100;
ALTER TABLE studio.payers            ALTER COLUMN id RESTART WITH 100;

