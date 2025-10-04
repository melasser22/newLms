-- Ensure the usage_event table exists before materialized views are refreshed.
create table if not exists usage_event (
  usage_event_id        bigserial primary key,
  rq_uid                uuid        not null,
  token_hash            varchar(64),
  payload               jsonb       not null,
  ext_product_id        bigint      not null,
  received_at           timestamptz not null default now(),
  processed             boolean     not null default true,
  status_code           varchar(32) not null,
  status_desc           varchar(128) not null,
  status_dtls           jsonb
);

comment on table usage_event is 'Immutable audit of Track Product Consumption requests & outcomes';
