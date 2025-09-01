-- Tenant overage and overage_enabled flag
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS overage_enabled BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS tenant_overage (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    feature_key VARCHAR(100) NOT NULL REFERENCES feature(key),
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ,
    quantity BIGINT NOT NULL,
    unit_price_minor BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    idem_key VARCHAR(255) UNIQUE
);
