-- Add overage columns to tier_feature_limit
ALTER TABLE tier_feature_limit ADD COLUMN IF NOT EXISTS allow_overage BOOLEAN;
ALTER TABLE tier_feature_limit ADD COLUMN IF NOT EXISTS overage_unit_price_minor BIGINT;
ALTER TABLE tier_feature_limit ADD COLUMN IF NOT EXISTS overage_currency VARCHAR(10);
