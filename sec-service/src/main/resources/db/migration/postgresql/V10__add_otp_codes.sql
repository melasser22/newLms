-- ============================================
-- V10: OTP Codes Table for Multi-Channel MFA
-- ============================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(32);

CREATE TABLE IF NOT EXISTS otp_codes (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash         VARCHAR(255) NOT NULL,
    mfa_method        VARCHAR(20)  NOT NULL,
    generated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMPTZ  NOT NULL,
    used_at           TIMESTAMPTZ,
    failed_attempts   INTEGER      NOT NULL DEFAULT 0,
    sent_to           VARCHAR(255),
    delivered         BOOLEAN      NOT NULL DEFAULT false,
    delivery_error    VARCHAR(500),

    CONSTRAINT ck_otp_method_valid CHECK (
        mfa_method IN ('EMAIL', 'SMS', 'CONSOLE')
    ),
    CONSTRAINT ck_otp_expires_after_generated CHECK (
        expires_at > generated_at
    )
);

CREATE INDEX ix_otp_user ON otp_codes(user_id);
CREATE INDEX ix_otp_code ON otp_codes(code_hash);
CREATE INDEX ix_otp_expires ON otp_codes(expires_at);

-- Auto-cleanup of expired OTPs (run hourly)
CREATE OR REPLACE FUNCTION cleanup_expired_otps()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM otp_codes
    WHERE expires_at < NOW() - INTERVAL '1 hour';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- View: Active OTPs
CREATE OR REPLACE VIEW active_otps AS
SELECT 
    id,
    user_id,
    mfa_method,
    generated_at,
    expires_at,
    sent_to,
    delivered,
    EXTRACT(EPOCH FROM (expires_at - NOW())) AS seconds_until_expiry
FROM otp_codes
WHERE used_at IS NULL
  AND expires_at > NOW()
ORDER BY generated_at DESC;

COMMENT ON TABLE otp_codes IS 
'Stores generated OTP codes for email/SMS/console MFA methods';

COMMENT ON COLUMN otp_codes.code_hash IS 
'Bcrypt hash of the 6-digit OTP code - never store plain text';
