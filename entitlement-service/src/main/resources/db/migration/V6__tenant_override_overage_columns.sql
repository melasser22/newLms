-- Add overage columns to tenant_feature_override
ALTER TABLE tenant_feature_override ADD COLUMN IF NOT EXISTS allow_overage BOOLEAN;
ALTER TABLE tenant_feature_override ADD COLUMN IF NOT EXISTS overage_unit_price_minor BIGINT;
ALTER TABLE tenant_feature_override ADD COLUMN IF NOT EXISTS overage_currency VARCHAR(10);
