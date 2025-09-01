-- Baseline schema: tenant, feature, product_tier
CREATE TABLE IF NOT EXISTS tenant (
    id UUID PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS feature (
    key VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS product_tier (
    id UUID PRIMARY KEY,
    name VARCHAR(255)
);
