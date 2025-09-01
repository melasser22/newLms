-- Tenant lifecycle fields and domain table
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS slug VARCHAR(100);
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS locale VARCHAR(20);
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS timezone VARCHAR(40);
CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_slug ON tenant(slug);
CREATE TABLE IF NOT EXISTS tenant_domain (
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL,
    PRIMARY KEY (tenant_id, domain)
);
