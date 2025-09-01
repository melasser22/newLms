-- Create ENUM types if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tenant_status') THEN
        CREATE TYPE tenant_status AS ENUM ('ACTIVE', 'SUSPENDED', 'ARCHIVED');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'key_status') THEN
        CREATE TYPE key_status AS ENUM ('ACTIVE', 'REVOKED');
    END IF;
END$$;

-- Table: tenant
-- Stores core information about each tenant.
CREATE TABLE IF NOT EXISTS tenant (
    tenant_id UUID PRIMARY KEY,
    tenant_slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    status tenant_status NOT NULL DEFAULT 'ACTIVE',
    tier_id TEXT, -- Foreign key relation managed by subscription/catalog service
    timezone TEXT NOT NULL DEFAULT 'UTC',
    locale TEXT NOT NULL DEFAULT 'en',
    domains TEXT[] DEFAULT '{}',
    overage_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_slug ON tenant(tenant_slug);
CREATE INDEX IF NOT EXISTS idx_tenant_status ON tenant(status);

-- Table: tenant_integration_key
-- Stores API keys for tenants to interact with the platform.
CREATE TABLE IF NOT EXISTS tenant_integration_key (
    key_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    key_prefix TEXT NOT NULL,
    key_hash BYTEA NOT NULL,
    name TEXT,
    scopes TEXT[] NOT NULL DEFAULT '{"*"}',
    rate_limit_per_min INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    status key_status NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (tenant_id, key_prefix)
);

CREATE INDEX IF NOT EXISTS idx_tenant_integration_key_prefix ON tenant_integration_key(key_prefix);
CREATE INDEX IF NOT EXISTS idx_tenant_integration_key_status ON tenant_integration_key(status);

-- Row-Level Security (RLS) for tenant_integration_key
-- This ensures that queries running under a specific tenant context can only see their own keys.
ALTER TABLE tenant_integration_key ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_integration_key FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_integration_key_isolation_policy
ON tenant_integration_key
FOR ALL
USING (tenant_id = current_setting('app.current_tenant', true)::UUID)
WITH CHECK (tenant_id = current_setting('app.current_tenant', true)::UUID);

-- Note: The 'tenant' table itself does not have RLS applied in this service,
-- as this service is the source of truth for tenants and may need to operate
-- across them for administrative purposes. Access control for tenant lifecycle
-- operations should be handled at the application/API layer via security rules.
-- RLS is applied to tenant-scoped sub-resources like keys.
