-- Read-model table used by the email usage service for daily aggregations.
CREATE TABLE IF NOT EXISTS email_event_log (
  id           BIGSERIAL PRIMARY KEY,
  tenant_id    VARCHAR(64) NOT NULL,
  event_type   VARCHAR(32) NOT NULL,
  occurred_at  TIMESTAMPTZ NOT NULL,
  send_id      BIGINT,
  message_id   VARCHAR(128),
  metadata     JSONB
);

CREATE INDEX IF NOT EXISTS idx_email_event_log_tenant_day
  ON email_event_log (tenant_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_email_event_log_type
  ON email_event_log (event_type);
