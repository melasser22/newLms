ALTER TABLE tenant_core.tenant ADD COLUMN tenant_slug VARCHAR(255);
UPDATE tenant_core.tenant SET tenant_slug = id::text;
ALTER TABLE tenant_core.tenant ALTER COLUMN tenant_slug SET NOT NULL;
ALTER TABLE tenant_core.tenant ADD CONSTRAINT uq_tenant_slug UNIQUE (tenant_slug);
