ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS tenant_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS security_tenant_id UUID;

CREATE INDEX IF NOT EXISTS idx_sub_tenant_code
    ON subscription(tenant_code)
    WHERE tenant_code IS NOT NULL;
