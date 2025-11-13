CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS internal_tenant_id UUID;

UPDATE subscription
   SET internal_tenant_id = uuid_generate_v5('6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'tenant:' || ext_customer_id::text)
 WHERE internal_tenant_id IS NULL;

ALTER TABLE subscription
    ALTER COLUMN internal_tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sub_internal_tenant
    ON subscription(internal_tenant_id)
    WHERE is_deleted = false;
