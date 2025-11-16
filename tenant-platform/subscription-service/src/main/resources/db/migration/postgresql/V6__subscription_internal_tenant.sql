ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS internal_tenant_id UUID;

-- Use random UUIDs instead of UUID v5
UPDATE subscription
   SET internal_tenant_id = gen_random_uuid()
 WHERE internal_tenant_id IS NULL;

ALTER TABLE subscription
    ALTER COLUMN internal_tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sub_internal_tenant
    ON subscription(internal_tenant_id)
    WHERE is_deleted = false;