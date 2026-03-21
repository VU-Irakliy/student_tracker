INSERT INTO studio.students (
    id, first_name, last_name, pricing_type, price_per_class, currency, timezone, class_type,
    holiday_mode, stopped_attending, debtor, created_at, updated_at, deleted
) VALUES
    (1, 'Ana', 'Garcia', 'PER_CLASS', 30.00, 'EUROS', 'SPAIN', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE),
    (2, 'Ivan', 'Petrov', 'PER_CLASS', 2000.00, 'RUBLES', 'RUSSIA_MOSCOW', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE);

INSERT INTO studio.class_sessions (
    id, student_id, class_date, start_time, timezone, duration_minutes,
    status, payment_status, price_charged, currency, is_one_off, created_at, updated_at, deleted
) VALUES
    (1, 1, '2026-03-05', '10:00:00', 'SPAIN', 60, 'COMPLETED', 'PAID', 30.00, 'EUROS', FALSE, NOW(), NOW(), FALSE),
    (2, 1, '2026-03-05', '11:00:00', 'SPAIN', 60, 'COMPLETED', 'PAID', 30.00, 'EUROS', FALSE, NOW(), NOW(), FALSE),
    (3, 1, '2026-03-10', '10:00:00', 'SPAIN', 60, 'COMPLETED', 'PAID', 35.00, 'EUROS', FALSE, NOW(), NOW(), FALSE),
    (4, 2, '2026-03-05', '12:00:00', 'RUSSIA_MOSCOW', 60, 'COMPLETED', 'PAID', 2000.00, 'RUBLES', FALSE, NOW(), NOW(), FALSE),
    (5, 1, '2026-03-07', '10:00:00', 'SPAIN', 60, 'SCHEDULED', 'UNPAID', 20.00, 'EUROS', FALSE, NOW(), NOW(), FALSE),
    (6, 1, '2026-03-08', '10:00:00', 'SPAIN', 60, 'CANCELLED', 'UNPAID', 15.00, 'EUROS', FALSE, NOW(), NOW(), FALSE);

INSERT INTO studio.package_purchases (
    id, student_id, total_classes, classes_remaining, amount_paid, currency, payment_date,
    description, created_at, updated_at, deleted
) VALUES
    (10, 2, 10, 10, 15000.00, 'RUBLES', '2026-03-10', 'March bundle', NOW(), NOW(), FALSE),
    (11, 2, 5, 5, 8000.00, 'RUBLES', '2026-03-01', 'March second bundle', NOW(), NOW(), FALSE);



