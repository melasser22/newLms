ALTER TABLE tenants ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255);
COMMENT ON COLUMN tenants.logo_url IS 'Tenant logo URL';
