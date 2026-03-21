INSERT INTO studio.students (
    id, first_name, last_name, pricing_type, price_per_class, currency, timezone, class_type,
    holiday_mode, stopped_attending, debtor, created_at, updated_at, deleted
) VALUES
    (1, 'Ana', 'Garcia', 'PER_CLASS', 30.00, 'EUROS', 'SPAIN', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE),
    (2, 'Ivan', 'Petrov', 'PACKAGE', NULL, 'RUBLES', 'RUSSIA_MOSCOW', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE),
    (3, 'Olga', 'Smirnova', 'PER_CLASS', 35.00, 'EUROS', 'SPAIN', 'CASUAL', FALSE, FALSE, TRUE, NOW(), NOW(), FALSE),
    (4, 'John', 'Smith', 'PER_CLASS', 40.00, 'DOLLARS', 'SPAIN', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE);

INSERT INTO studio.class_sessions (
    id, student_id, class_date, start_time, timezone, duration_minutes,
    status, payment_status, price_charged, currency, is_one_off, created_at, updated_at, deleted
) VALUES
    (1, 1, '2026-03-15', '20:00:00', 'SPAIN', 60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS', FALSE, NOW(), NOW(), FALSE),
    (2, 2, '2026-03-15', '20:00:00', 'RUSSIA_MOSCOW', 60, 'SCHEDULED', 'UNPAID', 2000.00, 'RUBLES', FALSE, NOW(), NOW(), FALSE),
    (3, 3, '2026-03-14', '12:00:00', 'SPAIN', 60, 'COMPLETED', 'PAID', 35.00, 'EUROS', FALSE, NOW(), NOW(), FALSE);



