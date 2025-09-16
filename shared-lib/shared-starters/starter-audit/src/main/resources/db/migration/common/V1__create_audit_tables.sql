-- Tables are created in the default schema configured for Flyway.
CREATE TABLE IF NOT EXISTS audit_logs (
  id               text PRIMARY KEY,
  ts_utc           timestamptz NOT NULL,
  x_tenant_id      text,
  actor_id         text,
  actor_username   text,
  action           text NOT NULL,
  entity_type      text,
  entity_id        text,
  outcome          text NOT NULL,
  data_class       text,
  sensitivity      text,
  resource_path    text,
  resource_method  text,
  correlation_id   text,
  span_id          text,
  message          text,
  payload          jsonb NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_ts    ON audit_logs (ts_utc);
CREATE INDEX IF NOT EXISTS idx_audit_logs_x_tenant ON audit_logs (x_tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);

CREATE TABLE  IF NOT EXISTS audit_outbox (
    id      UUID        PRIMARY KEY,
    payload JSONB       NOT NULL,
    status  VARCHAR(20) NOT NULL
);

ALTER TABLE audit_outbox ADD COLUMN IF NOT EXISTS correlation_id text;

