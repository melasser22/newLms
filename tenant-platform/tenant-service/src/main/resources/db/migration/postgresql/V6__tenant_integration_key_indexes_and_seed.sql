-- Partial unique: (tenant_id, key_id) must be unique while not deleted
CREATE UNIQUE INDEX IF NOT EXISTS uq_tik_tenant_key_not_deleted
  ON tenant_integration_key (tenant_id, key_id)
  WHERE is_deleted = FALSE;

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_tik_tenant         ON tenant_integration_key (tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_tik_status         ON tenant_integration_key (status)    WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_tik_expires_at     ON tenant_integration_key (expires_at) WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_tik_valid_from     ON tenant_integration_key (valid_from)  WHERE is_deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_tik_last_used_at   ON tenant_integration_key (last_used_at) WHERE is_deleted = FALSE;