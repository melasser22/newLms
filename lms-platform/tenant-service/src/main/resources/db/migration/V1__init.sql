create table if not exists tenant (
  tenant_id uuid primary key,
  tenant_slug text unique,
  name text,
  status text,
  tier_id text,
  timezone text default 'UTC',
  locale text default 'en',
  domains text[] default '{}',
  overage_enabled boolean not null default false,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create table if not exists tenant_integration_key (
  key_id uuid primary key,
  tenant_id uuid not null references tenant(tenant_id),
  key_prefix text not null,
  key_hash bytea not null,
  name text,
  scopes text[],
  rate_limit_per_min int,
  created_at timestamptz default now(),
  last_used_at timestamptz,
  expires_at timestamptz,
  status text,
  unique(tenant_id, key_prefix)
);

alter table tenant_integration_key enable row level security;
create policy tenant_key_policy on tenant_integration_key using (tenant_id = current_setting('app.current_tenant', true)::uuid);
