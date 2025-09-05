-- ===== (1) Ensure index exists (idempotent)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'i'
      AND c.relname = 'idx_tenant_integration_key_tenant_id'
      AND n.nspname = 'tenant_core'
  ) THEN
    CREATE INDEX idx_tenant_integration_key_tenant_id
      ON tenant_core.tenant_integration_key (tenant_id);
  END IF;
END $$;

-- ===== (2) Ensure tenant_slug column exists, backfill, and enforce constraints

-- 2.1 Add column if missing
ALTER TABLE tenant_core.tenant
  ADD COLUMN IF NOT EXISTS tenant_slug varchar(255);

-- 2.2 Backfill nulls with a clean slug from "name"
UPDATE tenant_core.tenant
SET tenant_slug = btrim(
                   regexp_replace(lower("name"), '[^a-z0-9]+', '-', 'g'),
                   '-'
                 )
WHERE tenant_slug IS NULL;

-- 2.3 Add UNIQUE constraint if missing
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

-- 2.4 Make column NOT NULL (safe after backfill)
ALTER TABLE tenant_core.tenant
  ALTER COLUMN tenant_slug SET NOT NULL;

-- ===== (3) Seed demo row (use explicit columns; let defaults handle status/overage_enabled)
INSERT INTO tenant_core.tenant (id, "name", tenant_slug)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'Demo Tenant',
  'demo-tenant'
)
ON CONFLICT (id) DO UPDATE SET
  "name" = EXCLUDED."name",
  tenant_slug = EXCLUDED.tenant_slug;
