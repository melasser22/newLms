-- Subscriptions and cache
DO $$ BEGIN
    CREATE TYPE subscription_status AS ENUM ('TRIALING','ACTIVE','PAST_DUE','CANCELED');
EXCEPTION WHEN duplicate_object THEN null; END $$;

CREATE TABLE IF NOT EXISTS tenant_subscription (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    tier_id UUID NOT NULL REFERENCES product_tier(id),
    status subscription_status NOT NULL,
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS subscription_item (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES tenant_subscription(id),
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS entitlement_cache (
    tenant_id UUID PRIMARY KEY REFERENCES tenant(id),
    snapshot TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),
    payload TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);
