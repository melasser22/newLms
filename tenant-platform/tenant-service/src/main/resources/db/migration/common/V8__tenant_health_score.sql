-- Tenant health scoring infrastructure

CREATE TABLE IF NOT EXISTS tenant_health_score (
    tenant_health_score_id BIGSERIAL PRIMARY KEY,
    tenant_id              INTEGER      NOT NULL,
    score                  INTEGER      NOT NULL CHECK (score BETWEEN 0 AND 100),
    risk_category          VARCHAR(32)  NOT NULL,
    feature_adoption_rate  NUMERIC(5,2) NOT NULL,
    login_frequency_score  NUMERIC(5,2) NOT NULL,
    user_engagement_score  NUMERIC(5,2) NOT NULL,
    usage_trend_percent    NUMERIC(6,2) NOT NULL,
    support_ticket_score   NUMERIC(5,2) NOT NULL,
    payment_history_score  NUMERIC(5,2) NOT NULL,
    api_health_score       NUMERIC(5,2) NOT NULL,
    evaluated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ths_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tenant_health_score_tenant ON tenant_health_score (tenant_id, evaluated_at);
CREATE INDEX IF NOT EXISTS idx_tenant_health_score_created ON tenant_health_score (created_at);

CREATE TABLE IF NOT EXISTS outbox_event (
    outbox_event_id BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    VARCHAR(128) NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished_tenant ON outbox_event (published, created_at);

COMMENT ON TABLE tenant_health_score IS 'Historical tenant health scores with metric breakdowns';
COMMENT ON TABLE outbox_event IS 'Transactional outbox for tenant events';
