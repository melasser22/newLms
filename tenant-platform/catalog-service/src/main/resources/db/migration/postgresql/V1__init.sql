create table if not exists feature (
  feature_key text primary key,
  description text
);

create table if not exists product_tier (
  tier_id text primary key,
  name text,
  description text,
  active boolean,
  is_default boolean,
  billing_external_ids jsonb,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists tier_feature_limit (
  tier_id text references product_tier(tier_id),
  feature_key text references feature(feature_key),
  enabled boolean,
  limit_value bigint,
  allow_overage boolean default false,
  overage_unit_price_minor bigint,
  overage_currency text default 'USD',
  primary key (tier_id, feature_key)
);

create table if not exists tenant_feature_override (
  tenant_id uuid,
  feature_key text,
  enabled boolean,
  limit_value bigint,
  allow_overage_override boolean,
  overage_unit_price_minor_override bigint,
  overage_currency_override text,
  primary key (tenant_id, feature_key)
);

alter table tenant_feature_override enable row level security;
create policy tenant_override_policy on tenant_feature_override using (tenant_id = current_setting('app.current_tenant', true)::uuid);
