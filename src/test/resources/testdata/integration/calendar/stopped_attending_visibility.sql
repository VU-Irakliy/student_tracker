DELETE FROM studio.class_sessions;
DELETE FROM studio.students;

INSERT INTO studio.students (
    id, first_name, last_name, phone_number, pricing_type, price_per_class, currency,
    timezone, class_type, start_date, holiday_mode, holiday_from, holiday_to,
    stopped_attending, notes, debtor, created_at, updated_at, deleted
) VALUES
    (1101, 'Active', 'Student', NULL, 'PER_CLASS', 30.00, 'EUROS',
     'SPAIN', 'EGE', NULL, FALSE, NULL, NULL,
     FALSE, NULL, FALSE, NOW(), NOW(), FALSE),
    (1102, 'Stopped', 'Student', NULL, 'PER_CLASS', 30.00, 'EUROS',
     'SPAIN', 'EGE', NULL, FALSE, NULL, NULL,
     TRUE, NULL, FALSE, NOW(), NOW(), FALSE);

INSERT INTO studio.class_sessions (
    id, student_id, weekly_schedule_id, class_date, start_time, timezone,
    duration_minutes, status, payment_status, price_charged, currency,
    package_purchase_id, is_one_off, note, created_at, updated_at, deleted
) VALUES
    (2101, 1102, NULL, DATEADD('DAY', -2, CURRENT_DATE), '10:00:00', 'SPAIN',
     60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS',
     NULL, TRUE, 'stopped student past session', NOW(), NOW(), FALSE),
    (2102, 1102, NULL, DATEADD('DAY', 2, CURRENT_DATE), '11:00:00', 'SPAIN',
     60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS',
     NULL, TRUE, 'stopped student future session', NOW(), NOW(), FALSE),
    (2103, 1101, NULL, DATEADD('DAY', 2, CURRENT_DATE), '12:00:00', 'SPAIN',
     60, 'SCHEDULED', 'UNPAID', 30.00, 'EUROS',
     NULL, TRUE, 'active student future session', NOW(), NOW(), FALSE);

