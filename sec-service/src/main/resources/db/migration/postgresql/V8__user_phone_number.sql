ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_tenant_phone
    ON users (tenant_id, phone_number)
    WHERE phone_number IS NOT NULL;
