ALTER TABLE audit_outbox ADD COLUMN IF NOT EXISTS correlation_id text;
