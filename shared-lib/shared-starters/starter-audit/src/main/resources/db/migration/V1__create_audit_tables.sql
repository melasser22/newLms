-- Flyway: V1__create_audit_tables.sql
CREATE TABLE IF NOT EXISTS setup.audit_logs (
  id               text PRIMARY KEY,
  ts_utc           timestamptz NOT NULL,
  tenant_id        text,
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

-- helpful indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_ts    ON setup.audit_logs (ts_utc);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant ON setup.audit_logs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON setup.audit_logs (entity_type, entity_id);

CREATE TABLE  IF NOT EXISTS setup.audit_outbox (
    id      UUID        PRIMARY KEY,
    payload JSONB       NOT NULL,
    status  VARCHAR(20) NOT NULL
);
