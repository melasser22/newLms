create table IF NOT EXISTS tier (
  id            bigserial primary key,
  code          varchar(64) not null,          -- BASIC, PRO, ENTERPRISE
  name_en       varchar(128) not null,
  name_ar       varchar(128) not null,
  description   text,
  rank_order    int not null default 0,        
  is_active     boolean not null default true,
  is_deleted    boolean not null default false,
  created_at    timestamptz not null default now(),
  created_by    varchar(128),
  updated_at    timestamptz,
  updated_by    varchar(128),
  constraint uq_tier_code unique (code)
);

create index idx_tier_active on tier(is_active) where is_deleted = false;
create index idx_tier_rank   on tier(rank_order);
