-- Tier feature limits and tenant overrides
CREATE TABLE IF NOT EXISTS tier_feature_limit (
    id UUID PRIMARY KEY,
    tier_id UUID NOT NULL REFERENCES product_tier(id),
    feature_key VARCHAR(100) NOT NULL REFERENCES feature(key),
    feature_limit BIGINT
);

CREATE TABLE IF NOT EXISTS tenant_feature_override (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    feature_key VARCHAR(100) NOT NULL REFERENCES feature(key),
    feature_limit BIGINT
);
