create extension if not exists pgcrypto;

do $$ begin
  if not exists (select 1 from pg_type where typname='tenant_status') then
    create type tenant_status as enum ('ACTIVE','SUSPENDED','ARCHIVED');
  end if;
end $$;

do $$ begin
  if not exists (select 1 from information_schema.tables where table_name = 'tenant') then
    create table tenant (
      tenant_id uuid primary key default gen_random_uuid(),
      tenant_slug text not null unique,
      name text not null,
      status tenant_status not null default 'ACTIVE',
      tier_id text,
      timezone text not null default 'UTC',
      locale text not null default 'en',
      domains text[] not null default '{}',
      overage_enabled boolean not null default false,
      created_at timestamptz not null default now(),
      updated_at timestamptz not null default now()
    );
  end if;
end $$;

do $$ begin
  if not exists (select 1 from pg_type where typname='key_status') then
    create type key_status as enum ('ACTIVE','REVOKED');
  end if;
end $$;

do $$ begin
  if not exists (select 1 from information_schema.tables where table_name = 'tenant_integration_key') then
    create table tenant_integration_key (
      key_id uuid primary key default gen_random_uuid(),
      tenant_id uuid not null references tenant(tenant_id) on delete cascade,
      key_prefix text not null,
      key_hash bytea not null,
      name text,
      scopes text[] not null default '{"*"}',
      rate_limit_per_min integer,
      created_at timestamptz not null default now(),
      last_used_at timestamptz,
      expires_at timestamptz,
      status key_status not null default 'ACTIVE',
      unique (tenant_id, key_prefix)
    );
  end if;
end $$;

create index if not exists ix_tenant_slug on tenant(tenant_slug);
create index if not exists ix_tenant_status on tenant(status);
