
CREATE TABLE IF NOT EXISTS tenant_outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64),
    type TEXT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS tenant_outbox_status_idx ON tenant_outbox(status, available_at);
