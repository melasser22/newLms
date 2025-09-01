CREATE INDEX idx_tenant_integration_key_tenant_id ON tenant_integration_key (tenant_id);

-- optional demo seed
INSERT INTO tenant (id, name, status, overage_enabled)
VALUES ('00000000-0000-0000-0000-000000000001', 'Demo Tenant', 'ACTIVE', FALSE)
ON CONFLICT DO NOTHING;

