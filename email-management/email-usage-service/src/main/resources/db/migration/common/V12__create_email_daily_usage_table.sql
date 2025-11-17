-- Create daily usage aggregate table for email usage service.
CREATE TABLE IF NOT EXISTS email_daily_usage (
  id BIGSERIAL PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  usage_date DATE NOT NULL,
  sent_count BIGINT NOT NULL DEFAULT 0,
  delivered_count BIGINT NOT NULL DEFAULT 0,
  bounced_count BIGINT NOT NULL DEFAULT 0,
  opened_count BIGINT NOT NULL DEFAULT 0,
  clicked_count BIGINT NOT NULL DEFAULT 0,
  spam_complaint_count BIGINT NOT NULL DEFAULT 0,
  deferred_count BIGINT NOT NULL DEFAULT 0,
  blocked_count BIGINT NOT NULL DEFAULT 0,
  quota_consumed BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usage_tenant_date
  ON email_daily_usage (tenant_id, usage_date);

CREATE INDEX IF NOT EXISTS idx_usage_date
  ON email_daily_usage (usage_date);
