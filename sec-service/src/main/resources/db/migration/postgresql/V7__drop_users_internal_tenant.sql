DROP INDEX IF EXISTS ix_users_internal_tenant;
ALTER TABLE users DROP COLUMN IF EXISTS internal_tenant_id;
