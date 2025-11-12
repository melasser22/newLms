-- ========= Outbox for domain events (to Kafka/Bus) =========
create table IF NOT EXISTS outbox_event (
  id             bigserial primary key,
  aggregate_type varchar(64) not null,       -- SUBSCRIPTION | ENTITLEMENT | PROVISIONING
  aggregate_id   varchar(128) not null,      -- id as string
  event_type     varchar(64) not null,       -- CREATED | UPDATED | STATUS_CHANGED | PROVISIONED
  payload        jsonb       not null,
  headers        jsonb,
  created_at     timestamptz not null default now(),
  processed_at   timestamptz
);
create index if not exists idx_outbox_unprocessed on outbox_event(aggregate_type, aggregate_id) where processed_at is null;

-- ========= Idempotent request keys for write paths (rqUID) =========
create table IF NOT EXISTS idempotent_request (
  idempotency_key  uuid primary key,         -- rqUID
  endpoint         varchar(128) not null,
  request_hash     varchar(128) not null,
  created_at       timestamptz not null default now()
);
