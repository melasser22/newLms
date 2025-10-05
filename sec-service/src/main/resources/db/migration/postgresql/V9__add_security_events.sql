-- ============================================
-- V9: Security Events & Monitoring
-- ============================================

CREATE TABLE IF NOT EXISTS security_events (
    id                 BIGSERIAL PRIMARY KEY,
    tenant_id          UUID,
    user_id            BIGINT,
    username           VARCHAR(255),
    event_type         VARCHAR(50)  NOT NULL,
    severity           VARCHAR(20)  NOT NULL,
    event_time         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ip_address         VARCHAR(64),
    user_agent         VARCHAR(512),
    resource           VARCHAR(100),
    action             VARCHAR(50),
    status             VARCHAR(20),
    details            TEXT,
    risk_score         INTEGER,
    flagged_for_review BOOLEAN      NOT NULL DEFAULT false,
    reviewed_at        TIMESTAMPTZ,
    reviewed_by        BIGINT,
    review_notes       TEXT,
    location           VARCHAR(255),
    device_fingerprint VARCHAR(255),

    CONSTRAINT ck_severity_valid CHECK (
        severity IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    ),
    CONSTRAINT ck_risk_score_range CHECK (
        risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 100)
    )
);

-- Indexes for fast querying
CREATE INDEX ix_sec_events_user ON security_events(user_id);
CREATE INDEX ix_sec_events_tenant ON security_events(tenant_id);
CREATE INDEX ix_sec_events_type ON security_events(event_type);
CREATE INDEX ix_sec_events_severity ON security_events(severity);
CREATE INDEX ix_sec_events_time ON security_events(event_time DESC);
CREATE INDEX ix_sec_events_ip ON security_events(ip_address);
CREATE INDEX ix_sec_events_flagged ON security_events(flagged_for_review)
    WHERE flagged_for_review = true;

-- Composite indexes for common queries
CREATE INDEX ix_sec_events_user_time ON security_events(user_id, event_time DESC);
CREATE INDEX ix_sec_events_tenant_time ON security_events(tenant_id, event_time DESC);
CREATE INDEX ix_sec_events_type_time ON security_events(event_type, event_time DESC);

-- ============================================
-- Views for Common Queries
-- ============================================

-- High-risk events requiring review
CREATE OR REPLACE VIEW high_risk_events AS
SELECT
    id,
    tenant_id,
    user_id,
    username,
    event_type,
    severity,
    event_time,
    ip_address,
    resource,
    action,
    risk_score,
    details
FROM security_events
WHERE (severity IN ('HIGH', 'CRITICAL') OR risk_score >= 75)
  AND reviewed_at IS NULL
ORDER BY event_time DESC;

-- Recent failed login attempts
CREATE OR REPLACE VIEW recent_failed_logins AS
SELECT
    user_id,
    username,
    tenant_id,
    ip_address,
    event_time,
    COUNT(*) OVER (
        PARTITION BY COALESCE(user_id::TEXT, username, ip_address)
        ORDER BY event_time
        RANGE BETWEEN INTERVAL '15 minutes' PRECEDING AND CURRENT ROW
    ) AS failure_count_15min
FROM security_events
WHERE event_type = 'LOGIN_FAILURE'
  AND event_time > NOW() - INTERVAL '1 hour'
ORDER BY event_time DESC;

-- ============================================
-- Functions for Anomaly Detection
-- ============================================

-- Detect brute force attempts
CREATE OR REPLACE FUNCTION detect_brute_force(
    p_ip_address VARCHAR,
    p_username VARCHAR DEFAULT NULL,
    p_time_window_minutes INTEGER DEFAULT 15,
    p_threshold INTEGER DEFAULT 5
)
RETURNS BOOLEAN AS $$
DECLARE
    failure_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO failure_count
    FROM security_events
    WHERE event_type = 'LOGIN_FAILURE'
      AND event_time > NOW() - (p_time_window_minutes || ' minutes')::INTERVAL
      AND (
          ip_address = p_ip_address
          OR (p_username IS NOT NULL AND username = p_username)
      );

    RETURN failure_count >= p_threshold;
END;
$$ LANGUAGE plpgsql;

-- Get user's recent login locations
CREATE OR REPLACE FUNCTION get_user_recent_locations(
    p_user_id BIGINT,
    p_limit INTEGER DEFAULT 10
)
RETURNS TABLE(
    ip_address VARCHAR,
    location VARCHAR,
    event_time TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT DISTINCT ON (e.ip_address)
        e.ip_address,
        e.location,
        e.event_time
    FROM security_events e
    WHERE e.user_id = p_user_id
      AND e.event_type = 'LOGIN_SUCCESS'
      AND e.event_time > NOW() - INTERVAL '90 days'
    ORDER BY e.ip_address, e.event_time DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Check for concurrent session anomaly
CREATE OR REPLACE FUNCTION check_concurrent_session_anomaly(
    p_user_id BIGINT,
    p_time_window_minutes INTEGER DEFAULT 5
)
RETURNS TABLE(
    unique_ips INTEGER,
    unique_locations INTEGER,
    event_times TIMESTAMPTZ[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(DISTINCT ip_address)::INTEGER,
        COUNT(DISTINCT location)::INTEGER,
        ARRAY_AGG(DISTINCT event_time ORDER BY event_time)
    FROM security_events
    WHERE user_id = p_user_id
      AND event_type = 'LOGIN_SUCCESS'
      AND event_time > NOW() - (p_time_window_minutes || ' minutes')::INTERVAL;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Cleanup Function
-- ============================================

CREATE OR REPLACE FUNCTION cleanup_old_security_events(
    p_retention_days INTEGER DEFAULT 90
)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    -- Keep high-severity events longer
    DELETE FROM security_events
    WHERE event_time < NOW() - (p_retention_days || ' days')::INTERVAL
      AND severity NOT IN ('HIGH', 'CRITICAL');

    GET DIAGNOSTICS deleted_count = ROW_COUNT;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- Comments
-- ============================================

COMMENT ON TABLE security_events IS
'Comprehensive security event log for audit, compliance, and threat detection';

COMMENT ON COLUMN security_events.risk_score IS
'Calculated risk score 0-100: INFO(0-24), LOW(25-49), MEDIUM(50-74), HIGH(75-99), CRITICAL(100)';

COMMENT ON VIEW high_risk_events IS
'Events requiring security team review (HIGH/CRITICAL severity or risk_score >= 75)';

COMMENT ON VIEW recent_failed_logins IS
'Failed login attempts with rolling 15-minute failure count for brute force detection';
