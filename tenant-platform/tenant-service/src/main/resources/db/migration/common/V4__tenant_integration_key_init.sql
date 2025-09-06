-- TENANT_INTEGRATION_KEY: per-tenant API credentials with expiry window

CREATE TABLE IF NOT EXISTS tenant_integration_key (
    tik_id         BIGSERIAL PRIMARY KEY,
    tenant_id      INTEGER      NOT NULL,
    key_id         VARCHAR(64)  NOT NULL,                 -- public identifier (e.g., KI_XXXX)
    key_secret     VARCHAR(255) NOT NULL,                 -- store HASH (bcrypt/argon2), never plaintext
    label          VARCHAR(128),

    scopes         TEXT[],                                -- optional list of scopes
    status         VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | SUSPENDED | REVOKED | EXPIRED

    valid_from     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ NOT NULL,                  -- hard expiry
    last_used_at   TIMESTAMPTZ,
    use_count      BIGINT      NOT NULL DEFAULT 0,

    daily_quota    INTEGER,                               -- optional enforcement hint
    meta           JSONB,

    is_deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(128),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(128),

    CONSTRAINT fk_tik_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,

    CONSTRAINT ck_tik_status
        CHECK (status IN ('ACTIVE','SUSPENDED','REVOKED','EXPIRED')),

    CONSTRAINT ck_tik_expiry_window
        CHECK (expires_at > valid_from)
);

-- Keep updated_at in sync (re-uses the generic trigger function created in V1)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger WHERE tgname = 'set_timestamp_on_tenant_integration_key'
  ) THEN
    CREATE TRIGGER set_timestamp_on_tenant_integration_key
    BEFORE UPDATE ON tenant_integration_key
    FOR EACH ROW
    EXECUTE FUNCTION trg_set_timestamp();
  END IF;
END $$;
