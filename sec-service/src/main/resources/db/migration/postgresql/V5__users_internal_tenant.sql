CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS internal_tenant_id UUID;

UPDATE users
   SET internal_tenant_id = tenant_id
 WHERE internal_tenant_id IS NULL;

ALTER TABLE users
    ALTER COLUMN internal_tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_users_internal_tenant
    ON users(internal_tenant_id);
