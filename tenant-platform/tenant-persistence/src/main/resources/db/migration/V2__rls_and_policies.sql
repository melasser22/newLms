ALTER TABLE tenant_integration_key ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_integration_key_tenant_isolation
    ON tenant_integration_key
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

