-- Partial unique indexes that respect soft-delete
-- Ensures you can reuse code/name after a logical delete (is_deleted = true)

-- code is unique while not deleted
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_code_not_deleted
  ON tenants (code)
  WHERE is_deleted = FALSE;

-- name is case-insensitive unique while not deleted (functional index on lower(name))
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_lower_name_not_deleted
  ON tenants (LOWER(name))
  WHERE is_deleted = FALSE;

-- Helpful filtered indexes
CREATE INDEX IF NOT EXISTS idx_tenants_active_not_deleted
  ON tenants (active)
  WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_tenants_created_at_desc
  ON tenants (created_at DESC)
  WHERE is_deleted = FALSE;