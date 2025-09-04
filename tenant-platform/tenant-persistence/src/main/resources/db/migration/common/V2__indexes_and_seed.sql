CREATE INDEX idx_tenant_integration_key_tenant_id ON tenant_core.tenant_integration_key (tenant_id);

-- optional demo seed
INSERT INTO tenant_core.tenant (id, name, status, overage_enabled)
SELECT '00000000-0000-0000-0000-000000000001', 'Demo Tenant', 'ACTIVE', FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM tenant_core.tenant WHERE id = '00000000-0000-0000-0000-000000000001'
);

