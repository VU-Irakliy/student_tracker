INSERT INTO studio.students (
    id, first_name, last_name, pricing_type, price_per_class, currency, timezone, class_type,
    holiday_mode, stopped_attending, debtor, created_at, updated_at, deleted
) VALUES
    (1, 'Ivan', 'Petrov', 'PACKAGE', NULL, 'RUBLES', 'RUSSIA_MOSCOW', 'CASUAL', FALSE, FALSE, FALSE, NOW(), NOW(), FALSE);

INSERT INTO studio.package_purchases (
    id, student_id, total_classes, classes_remaining, amount_paid, currency, payment_date,
    description, created_at, updated_at, deleted
) VALUES
    (10, 1, 10, 10, 15000.00, 'RUBLES', '2026-03-01', 'March bundle', NOW(), NOW(), FALSE),
    (11, 1, 5, 0, 7000.00, 'RUBLES', '2026-02-01', 'Exhausted bundle', NOW(), NOW(), FALSE);



