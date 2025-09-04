-- V3__add_tenant_slug.sql  (idempotent & safe)

-- 1) Add column only if missing
ALTER TABLE tenant_core.tenant
  ADD COLUMN IF NOT EXISTS tenant_slug varchar(255);

-- 2) Backfill NULLs 
UPDATE tenant_core.tenant
SET tenant_slug = id::text
WHERE tenant_slug IS NULL;

-- 3) Add UNIQUE constraint only if missing
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    WHERE c.conname = 'uq_tenant_slug'
      AND t.relname = 'tenant'
      AND n.nspname = 'tenant_core'
  ) THEN
    ALTER TABLE tenant_core.tenant
      ADD CONSTRAINT uq_tenant_slug UNIQUE (tenant_slug);
  END IF;
END $$;

-- 4) Enforce NOT NULL 
ALTER TABLE tenant_core.tenant
  ALTER COLUMN tenant_slug SET NOT NULL;
