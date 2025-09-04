CREATE SCHEMA IF NOT EXISTS tenant_core;

CREATE TYPE tenant_status AS ENUM ('ACTIVE', 'INACTIVE');

CREATE TYPE key_status AS ENUM ('ACTIVE', 'REVOKED');

CREATE TABLE tenant_core.tenant (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status tenant_status NOT NULL DEFAULT 'ACTIVE',
    overage_enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE tenant_core.tenant_integration_key (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant_core.tenant(id),
    name VARCHAR(255) NOT NULL,
    key_value VARCHAR(255) NOT NULL,
    status key_status NOT NULL DEFAULT 'ACTIVE'
);

