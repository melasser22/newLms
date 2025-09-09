CREATE TABLE IF NOT EXISTS privileges (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    UUID         NOT NULL,                   
    code         VARCHAR(100) NOT NULL,                   -- eg: USER_READ, USER_CREATE, ROLE_ASSIGN
    resource     VARCHAR(100) NOT NULL,                   -- eg: USER, ROLE, PRIVILEGE, TENANT, AUDIT_LOG
    action       VARCHAR(50)  NOT NULL,                   -- eg: READ, CREATE, UPDATE, DELETE, ASSIGN, EXPORT
    name         VARCHAR(200) NOT NULL,                   -- Name
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_privileges_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_priv_action CHECK (action ~ '^[A-Z_]+$'),
    CONSTRAINT ck_priv_code   CHECK (code   ~ '^[A-Z0-9_]+$')
);

DROP TRIGGER IF EXISTS trg_priv_set_updated ON privileges;
CREATE TRIGGER trg_priv_set_updated
BEFORE UPDATE ON privileges
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS ix_privileges_tenant_resource_action
    ON privileges (tenant_id, resource, action);

-- =========
-- ROLE_PRIVILEGES 
-- =========
CREATE TABLE IF NOT EXISTS role_privileges (
    role_id      BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    privilege_id BIGINT NOT NULL REFERENCES privileges(id) ON DELETE CASCADE,
    granted_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by   BIGINT,  -- User who gives the privilege 
    PRIMARY KEY (role_id, privilege_id)
);

CREATE INDEX IF NOT EXISTS ix_role_priv_role ON role_privileges (role_id);
CREATE INDEX IF NOT EXISTS ix_role_priv_priv ON role_privileges (privilege_id);

-- =========
-- USER_PRIVILEGES (Overrides )
-- =========
CREATE TABLE IF NOT EXISTS user_privileges (
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    privilege_id BIGINT NOT NULL REFERENCES privileges(id) ON DELETE CASCADE,
    is_granted   BOOLEAN NOT NULL,  -- TRUE:False
    noted_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    noted_by     BIGINT,  
    PRIMARY KEY (user_id, privilege_id)
);

CREATE INDEX IF NOT EXISTS ix_user_priv_user ON user_privileges (user_id);

-- =========
-- VIEW: effective_privileges to preview user privileges
CREATE OR REPLACE VIEW effective_privileges AS
SELECT
    u.id                AS user_id,
    u.tenant_id         AS tenant_id,
    p.id                AS privilege_id,
    p.code,
    p.resource,
    p.action,
    CASE
        WHEN up.is_granted = FALSE THEN FALSE
        WHEN up.is_granted = TRUE  THEN TRUE
        WHEN up.is_granted IS NULL AND rp.privilege_id IS NOT NULL THEN TRUE
        ELSE FALSE
    END AS is_effective
FROM users u
CROSS JOIN privileges p
LEFT JOIN user_privileges up
  ON up.user_id = u.id AND up.privilege_id = p.id
LEFT JOIN user_roles ur
  ON ur.user_id = u.id
LEFT JOIN role_privileges rp
  ON rp.role_id = ur.role_id AND rp.privilege_id = p.id;
