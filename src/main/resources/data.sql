INSERT INTO transaction_limits (limit_key, limit_value, description, active, created_at, updated_at)
VALUES ('MIN_TRANSACTION_AMOUNT', 1.00, 'Minimum amount per transaction', true, NOW(), NOW())
ON CONFLICT (limit_key) DO NOTHING;

INSERT INTO transaction_limits (limit_key, limit_value, description, active, created_at, updated_at)
VALUES ('MAX_SINGLE_TRANSACTION', 5000000.00, 'Maximum amount per single transaction', true, NOW(), NOW())
ON CONFLICT (limit_key) DO NOTHING;

INSERT INTO transaction_limits (limit_key, limit_value, description, active, created_at, updated_at)
VALUES ('DAILY_TRANSACTION_LIMIT', 10000000.00, 'Maximum total transactions per day', true, NOW(), NOW())
ON CONFLICT (limit_key) DO NOTHING;