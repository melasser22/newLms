CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS internal_tenant_id UUID;

UPDATE tenants
   SET internal_tenant_id = uuid_generate_v5('6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'tenant:' || code)
 WHERE internal_tenant_id IS NULL;

ALTER TABLE tenants
    ALTER COLUMN internal_tenant_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_tenants_internal_tenant
    ON tenants(internal_tenant_id)
    WHERE is_deleted = false;
