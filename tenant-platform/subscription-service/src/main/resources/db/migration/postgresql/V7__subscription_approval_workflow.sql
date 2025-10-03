-- Subscription approval workflow schema changes
ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(32) DEFAULT 'PENDING_APPROVAL',
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS submitted_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(128),
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT,
    ADD COLUMN IF NOT EXISTS admin_user_id BIGINT;

ALTER TABLE subscription
    ALTER COLUMN approval_status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_subscription_approval_status
    ON subscription(approval_status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_subscription_submitted_at
    ON subscription(submitted_at DESC);

CREATE TABLE IF NOT EXISTS subscription_approval_request (
    approval_request_id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(subscription_id) ON DELETE CASCADE,
    request_type VARCHAR(32) NOT NULL DEFAULT 'NEW_SUBSCRIPTION',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(16) DEFAULT 'NORMAL',
    risk_score INTEGER,
    risk_level VARCHAR(16),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    requested_by VARCHAR(128),
    due_date TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    approved_by VARCHAR(128),
    approver_email VARCHAR(255),
    approval_notes TEXT,
    rejected_at TIMESTAMPTZ,
    rejected_by VARCHAR(128),
    rejection_reason VARCHAR(64),
    rejection_notes TEXT,
    tenant_info_json JSONB,
    subscription_info_json JSONB,
    email_verified BOOLEAN,
    email_risk_score INTEGER,
    domain_age INTEGER,
    domain_reputation VARCHAR(32),
    crm_customer_id VARCHAR(64),
    customer_segment VARCHAR(32),
    requires_additional_review BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_approval_request_status
    ON subscription_approval_request(status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_approval_request_priority
    ON subscription_approval_request(priority, status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_approval_request_due_date
    ON subscription_approval_request(due_date)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_approval_request_subscription
    ON subscription_approval_request(subscription_id);

CREATE TABLE IF NOT EXISTS subscription_activity_log (
    activity_log_id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES subscription(subscription_id) ON DELETE CASCADE,
    activity_type VARCHAR(64) NOT NULL,
    description TEXT,
    performed_by VARCHAR(128) NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX IF NOT EXISTS idx_activity_log_subscription
    ON subscription_activity_log(subscription_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_log_type
    ON subscription_activity_log(activity_type, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_log_performed_by
    ON subscription_activity_log(performed_by);

CREATE TABLE IF NOT EXISTS blocked_customers (
    blocked_customer_id BIGSERIAL PRIMARY KEY,
    ext_customer_id BIGINT,
    email VARCHAR(255),
    domain VARCHAR(255),
    ip_address VARCHAR(45),
    block_reason VARCHAR(64) NOT NULL,
    block_notes TEXT,
    blocked_by VARCHAR(128) NOT NULL,
    blocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unblocked_by VARCHAR(128),
    unblocked_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_blocked_identifier CHECK (
        ext_customer_id IS NOT NULL OR email IS NOT NULL OR domain IS NOT NULL OR ip_address IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_blocked_customers_email
    ON blocked_customers(email)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_blocked_customers_domain
    ON blocked_customers(domain)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_blocked_customers_ext_id
    ON blocked_customers(ext_customer_id)
    WHERE is_active = TRUE;

CREATE TABLE IF NOT EXISTS auto_approval_rule (
    rule_id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL,
    rule_description TEXT,
    rule_type VARCHAR(32) NOT NULL,
    conditions JSONB NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_auto_approval_rule_active
    ON auto_approval_rule(is_active, priority DESC);
