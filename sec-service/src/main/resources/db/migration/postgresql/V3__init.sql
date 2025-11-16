CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID        NOT NULL,
    username        VARCHAR(120) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT users_email_ck CHECK (position('@' in email) > 1)
);

-- Unique per tenant
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_tenant_username
    ON users (tenant_id, username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_tenant_email
    ON users (tenant_id, email);

-- Helpful lookups
CREATE INDEX IF NOT EXISTS ix_users_tenant_id ON users (tenant_id);
CREATE INDEX IF NOT EXISTS ix_users_last_login ON users (last_login_at);

-- keep updated_at in sync
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_set_updated ON users;
CREATE TRIGGER trg_users_set_updated
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ==============
-- ROLES
-- ==============
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   UUID        NOT NULL,
    code        VARCHAR(64) NOT NULL,   -- e.g. ADMIN, USER
    name        VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_tenant_code
    ON roles (tenant_id, code);

DROP TRIGGER IF EXISTS trg_roles_set_updated ON roles;
CREATE TRIGGER trg_roles_set_updated
BEFORE UPDATE ON roles
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- ==============
-- USER_ROLES (M2M)
-- ==============
CREATE TABLE IF NOT EXISTS user_roles (
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id   BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS ix_user_roles_role_id ON user_roles (role_id);

-- ==============
-- REFRESH TOKENS
-- ==============
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token          VARCHAR(255) NOT NULL,     -- store opaque or JWT string
    issued_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ  NOT NULL,
    revoked_at     TIMESTAMPTZ,
    rotated_from   VARCHAR(255),              -- previous token id/string if rotating
    user_agent     VARCHAR(512),
    ip_address     VARCHAR(45),
    CONSTRAINT ck_refresh_not_past CHECK (expires_at > issued_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_expiry ON refresh_tokens (expires_at);

-- ==============
-- PASSWORD RESET TOKENS (one-time)
-- ==============
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token        VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_reset_not_past CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_password_reset_token ON password_reset_tokens (token);
CREATE INDEX IF NOT EXISTS ix_password_reset_user ON password_reset_tokens (user_id);
CREATE INDEX IF NOT EXISTS ix_password_reset_expiry ON password_reset_tokens (expires_at);

-- ==============
-- FEDERATED IDENTITIES (Google/Okta/etc.)
-- ==============
CREATE TABLE IF NOT EXISTS federated_identities (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id         UUID         NOT NULL,
    provider          VARCHAR(64)  NOT NULL,  -- e.g. 'google', 'okta'
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- A federated account must be unique for a given provider
CREATE UNIQUE INDEX IF NOT EXISTS ux_fed_identity_provider_user
    ON federated_identities (provider, provider_user_id);

-- Optional helpful index for tenant boundary checks
CREATE INDEX IF NOT EXISTS ix_fed_identity_tenant_user
    ON federated_identities (tenant_id, user_id);
