-- Flyway: V2__rename_tenant_id_to_x_tenant_id.sql
ALTER TABLE IF EXISTS setup.audit_logs
  ADD COLUMN IF NOT EXISTS x_tenant_id text;

UPDATE setup.audit_logs SET x_tenant_id = tenant_id WHERE x_tenant_id IS NULL;

DROP INDEX IF EXISTS idx_audit_logs_tenant;
ALTER TABLE IF EXISTS setup.audit_logs DROP COLUMN IF EXISTS tenant_id;
CREATE INDEX IF NOT EXISTS idx_audit_logs_x_tenant ON setup.audit_logs (x_tenant_id);
