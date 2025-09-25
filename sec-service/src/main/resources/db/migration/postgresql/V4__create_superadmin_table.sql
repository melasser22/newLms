-- ============================================================
--  Superadmins Schema & Supporting Tables
--  Author: Ejada
--  Purpose: Manage platform-level superadmin accounts
--  Notes:
--    - Default superadmin password: Admin@123!
--    - Change this password immediately after first login
-- ============================================================

-- ===================== superadmins ==========================
CREATE TABLE IF NOT EXISTS superadmins (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP,
    role VARCHAR(64) NOT NULL DEFAULT 'EJADA_OFFICER',
    first_login_completed BOOLEAN DEFAULT FALSE,
    password_changed_at TIMESTAMP,
    password_expires_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255)
);

-- Add extended columns (if not already present)
ALTER TABLE superadmins
    ADD COLUMN IF NOT EXISTS first_login_completed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS password_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255);

-- Indexes for faster lookup
CREATE INDEX IF NOT EXISTS idx_superadmins_username ON superadmins(username);
CREATE INDEX IF NOT EXISTS idx_superadmins_email ON superadmins(email);

-- Default superadmin account (password: Admin@123!)
INSERT INTO superadmins (username, email, password_hash, role, created_by)
VALUES (
    'superadmin',
    'admin@ejada.com',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a',
    'EJADA_OFFICER',
    'system'
)
ON CONFLICT (username) DO NOTHING;


-- ================= superadmin_password_history ==============
CREATE TABLE IF NOT EXISTS superadmin_password_history (
    id BIGSERIAL PRIMARY KEY,
    superadmin_id BIGINT NOT NULL REFERENCES superadmins(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_password_history_superadmin 
    ON superadmin_password_history(superadmin_id);


-- ================== superadmin_audit_logs ===================
CREATE TABLE IF NOT EXISTS superadmin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    superadmin_id BIGINT REFERENCES superadmins(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_superadmin 
    ON superadmin_audit_logs(superadmin_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp 
    ON superadmin_audit_logs(timestamp);

-- ============================================================
--  End of superadmin schema
-- ============================================================
