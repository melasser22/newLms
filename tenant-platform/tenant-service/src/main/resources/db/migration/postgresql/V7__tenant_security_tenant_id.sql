ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS security_tenant_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_security_tenant_id
    ON tenants (security_tenant_id)
    WHERE security_tenant_id IS NOT NULL;
