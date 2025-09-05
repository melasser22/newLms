create schema if not exists tenant_subscription;

create table if not exists tenant_subscription.subscription (
  subscription_id uuid primary key,
  tenant_id uuid not null,
  tier_id text,
  status text,
  active boolean default true,
  seats int,
  currency text,
  period_start timestamptz,
  period_end timestamptz,
  trial_end timestamptz,
  cancel_at timestamptz,
  cancel_at_period_end boolean,
  provider text,
  external_subscription_id text,
  external_account_id text,
  latest_invoice_id text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists tenant_subscription.subscription_item (
  item_id uuid primary key,
  subscription_id uuid references tenant_subscription.subscription(subscription_id),
  feature_key text,
  quantity bigint,
  unique(subscription_id, feature_key)
);

alter table tenant_subscription.subscription enable row level security;
alter table tenant_subscription.subscription_item enable row level security;

create policy tenant_sub_policy on tenant_subscription.subscription using (tenant_id = current_setting('app.current_tenant', true)::uuid);
create policy tenant_sub_item_policy on tenant_subscription.subscription_item using (subscription_id in (select subscription_id from tenant_subscription.subscription where tenant_id = current_setting('app.current_tenant', true)::uuid));

