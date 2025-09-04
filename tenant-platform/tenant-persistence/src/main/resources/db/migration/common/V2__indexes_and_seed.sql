-- Ensure the lookup index exists without failing if it is already present
CREATE INDEX IF NOT EXISTS tenant_core.idx_tenant_integration_key_tenant_id
    ON tenant_core.tenant_integration_key (tenant_id);

-- Optional demo seed
INSERT INTO tenant_core.tenant (id, name, status, overage_enabled)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Tenant', 'ACTIVE', FALSE)
ON CONFLICT (id) DO NOTHING;

