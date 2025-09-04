create schema if not exists tenant_billing;

create table if not exists tenant_billing.tenant_overage (
  overage_id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null,
  subscription_id uuid,
  feature_key text not null,
  quantity bigint not null check (quantity > 0),
  unit_price_minor bigint not null default 0,
  currency text not null default 'USD',
  occurred_at timestamptz not null default now(),
  period_start timestamptz not null,
  period_end timestamptz not null,
  status text not null default 'RECORDED',
  idempotency_key text,
  metadata jsonb not null default '{}'::jsonb
);

create index if not exists idx_overage_tenant_time on tenant_billing.tenant_overage(tenant_id, occurred_at desc);
create index if not exists idx_overage_tenant_feature on tenant_billing.tenant_overage(tenant_id, feature_key);
create unique index if not exists idx_overage_idem on tenant_billing.tenant_overage(tenant_id, idempotency_key) where idempotency_key is not null;

alter table tenant_billing.tenant_overage enable row level security;
create policy tenant_overage_policy on tenant_billing.tenant_overage using (tenant_id = current_setting('app.current_tenant', true)::uuid);
