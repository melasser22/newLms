ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS internal_tenant_id UUID;

-- Use random UUIDs instead of UUID v5 (no extension required)
UPDATE tenants
   SET internal_tenant_id = gen_random_uuid()
 WHERE internal_tenant_id IS NULL;

ALTER TABLE tenants
    ALTER COLUMN internal_tenant_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenants_internal_tenant
    ON tenants(internal_tenant_id)
    WHERE is_deleted = false;