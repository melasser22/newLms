-- Ensure the usage_event table exists so materialized views can aggregate from it even
-- when the Billing Service migrations have not been executed yet (e.g. local setups
-- where the Analytics Service starts first). The definition mirrors the Billing
-- Service schema and is idempotent so running both migrations is safe.
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

create materialized view if not exists mv_tenant_feature_usage_daily as
select
  coalesce((payload ->> 'tenantId')::bigint, 0) as tenant_id,
  coalesce(payload ->> 'featureKey', 'unknown') as feature_key,
  date_trunc('day', received_at) as usage_day,
  count(*) as event_count,
  sum(coalesce(nullif(payload ->> 'usageAmount', '')::numeric, 1)) as total_usage,
  max(coalesce(nullif(payload ->> 'planLimit', '')::numeric, 0)) as plan_limit
from usage_event
where processed = true
  and status_code = 'SUCCESS'
group by 1,2,3;

create materialized view if not exists mv_tenant_usage_summary as
select
  tenant_id,
  feature_key,
  date_trunc('month', usage_day) as usage_period,
  sum(total_usage) as total_usage,
  sum(event_count) as event_count,
  max(plan_limit) as plan_limit,
  max(usage_day) as last_event_at
from mv_tenant_feature_usage_daily
group by 1,2,3;

create materialized view if not exists mv_tenant_peak_usage_hourly as
select
  coalesce((payload ->> 'tenantId')::bigint, 0) as tenant_id,
  coalesce(payload ->> 'featureKey', 'unknown') as feature_key,
  date_trunc('hour', received_at) as usage_hour,
  count(*) as event_count,
  sum(coalesce(nullif(payload ->> 'usageAmount', '')::numeric, 1)) as total_usage
from usage_event
where processed = true
  and status_code = 'SUCCESS'
group by 1,2,3;

create index if not exists idx_mv_tenant_feature_usage_daily on mv_tenant_feature_usage_daily(tenant_id, feature_key, usage_day);
create index if not exists idx_mv_tenant_usage_summary on mv_tenant_usage_summary(tenant_id, feature_key, usage_period);
create index if not exists idx_mv_tenant_peak_usage_hourly on mv_tenant_peak_usage_hourly(tenant_id, feature_key, usage_hour);
