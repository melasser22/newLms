ALTER TABLE tenant ADD COLUMN tenant_slug VARCHAR(255);
UPDATE tenant SET tenant_slug = id::text;
ALTER TABLE tenant ALTER COLUMN tenant_slug SET NOT NULL;
ALTER TABLE tenant ADD CONSTRAINT uq_tenant_slug UNIQUE (tenant_slug);
