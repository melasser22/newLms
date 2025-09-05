-- Enable RLS safely
ALTER TABLE tenant_core.tenant_integration_key ENABLE ROW LEVEL SECURITY;

-- Drop existing policy if it exists, then recreate
DROP POLICY IF EXISTS tenant_integration_key_tenant_isolation
  ON tenant_core.tenant_integration_key;

CREATE POLICY tenant_integration_key_tenant_isolation
  ON tenant_core.tenant_integration_key
  USING (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
  );
