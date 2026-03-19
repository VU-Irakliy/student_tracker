INSERT INTO studio.students (
    id, first_name, last_name, pricing_type, price_per_class, currency, timezone, class_type,
    holiday_mode, stopped_attending, debtor, created_at, updated_at, deleted
) VALUES
    (1, 'Ana', 'Garcia', 'PER_CLASS', 30.00, 'EUROS', 'SPAIN', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE),
    (2, 'Ivan', 'Petrov', 'PACKAGE', NULL, 'RUBLES', 'RUSSIA_MOSCOW', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE),
    (30, 'Timezone', 'Student', 'PER_CLASS', 40.00, 'RUBLES', 'RUSSIA_MOSCOW', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE);

INSERT INTO studio.package_purchases (
    id, student_id, total_classes, classes_remaining, amount_paid, currency, payment_date,
    description, created_at, updated_at, deleted
) VALUES
    (1, 2, 10, 8, 15000.00, 'RUBLES', '2026-03-01', 'March bundle', NOW(), NOW(), FALSE),
    (2, 2, 5, 0, 8000.00, 'RUBLES', '2026-02-01', 'Exhausted bundle', NOW(), NOW(), FALSE);

INSERT INTO studio.class_sessions (
    id, student_id, class_date, start_time, timezone, duration_minutes,
    status, payment_status, price_charged, currency, package_purchase_id,
    is_one_off, note, created_at, updated_at, deleted
) VALUES
    (10, 1, '2026-03-15', '10:00:00', 'SPAIN', 60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (11, 1, '2026-03-16', '10:00:00', 'SPAIN', 60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (12, 1, '2026-03-14', '10:00:00', 'SPAIN', 60, 'SCHEDULED', 'PAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (20, 2, '2026-03-15', '10:00:00', 'RUSSIA_MOSCOW', 60, 'SCHEDULED', 'UNPAID', NULL, 'RUBLES', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (22, 2, '2026-03-15', '12:00:00', 'RUSSIA_MOSCOW', 60, 'SCHEDULED', 'PACKAGE', NULL, 'RUBLES', 1, FALSE, NULL, NOW(), NOW(), FALSE),
    (30, 1, '2026-03-18', '10:00:00', 'SPAIN', 60, 'CANCELLED', 'UNPAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (100, 1, '2026-03-20', '09:00:00', 'SPAIN', 90, 'COMPLETED', 'UNPAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (101, 1, '2026-03-20', '11:00:00', 'SPAIN', 30, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (102, 1, '2026-03-21', '10:00:00', 'SPAIN', 60, 'COMPLETED', 'UNPAID', 30.00, 'EUROS', NULL, FALSE, NULL, NOW(), NOW(), FALSE),
    (300, 30, '2026-01-15', '10:00:00', 'RUSSIA_MOSCOW', 60, 'SCHEDULED', 'UNPAID', 40.00, 'RUBLES', NULL, FALSE, NULL, NOW(), NOW(), FALSE);

