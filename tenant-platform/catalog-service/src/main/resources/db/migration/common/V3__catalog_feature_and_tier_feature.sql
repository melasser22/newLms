-- =========================
-- FEATURE (stand-alone)
-- =========================
create table IF NOT EXISTS feature (
  id             bigserial primary key,
  key            varchar(96)  not null,       -- e.g. CATALOG.PRODUCTS, INVENTORY.WAREHOUSES
  name_en        varchar(128) not null,
  name_ar        varchar(128) not null,
  description    text,
  category       varchar(64),                 -- logical grouping for UI/filtering
  is_metered     boolean not null default false,
  is_active      boolean not null default true,
  is_deleted     boolean not null default false,
  created_at     timestamptz not null default now(),
  created_by     varchar(128),
  updated_at     timestamptz,
  updated_by     varchar(128),
  constraint uq_feature_key unique (key)
);

create index idx_feature_active on feature(is_active) where is_deleted = false;
create index idx_feature_category on feature(category);

-- =========================
-- TIER_FEATURE (policy & limits per tier/feature)
-- =========================
create table IF NOT EXISTS tier_feature (
  id                   bigserial primary key,
  tier_id              bigint not null,
  feature_id           bigint not null,

  enabled              boolean not null default true,
  enforcement          varchar(24) not null default 'ALLOW',       -- ALLOW | BLOCK | ALLOW_WITH_OVERAGE

  soft_limit           numeric(18,3),                              -- warn threshold (nullable)
  hard_limit           numeric(18,3),                              -- cap (nullable unless BLOCK)
  limit_window         varchar(24),                                -- DAILY | MONTHLY | QUARTERLY | YEARLY | LIFETIME
  measure_unit         varchar(24),                                -- REQUESTS | ITEMS | MB | GB | POINTS | ...

  reset_cron           varchar(64),                                -- optional cron for resets (if window is custom)

  overage_enabled      boolean not null default false,
  overage_unit_price   numeric(18,4),                              -- required if overage_enabled=true
  overage_currency     varchar(3) default 'SAR',

  meta                 jsonb,                                      -- free-form settings
  is_deleted           boolean not null default false,
  created_at           timestamptz not null default now(),
  created_by           varchar(128),
  updated_at           timestamptz,
  updated_by           varchar(128),

  constraint uq_tier_feature unique (tier_id, feature_id),
  constraint fk_tf_tier    foreign key (tier_id)    references tier(id)    on delete cascade,
  constraint fk_tf_feature foreign key (feature_id) references feature(id) on delete cascade,

  -- Guard rails
  constraint ck_tf_enforcement check (enforcement in ('ALLOW','BLOCK','ALLOW_WITH_OVERAGE')),
  constraint ck_tf_window      check (limit_window is null or limit_window in ('DAILY','MONTHLY','QUARTERLY','YEARLY','LIFETIME')),
  constraint ck_tf_soft_hard   check (
      soft_limit is null or hard_limit is null or hard_limit >= soft_limit
  ),
  constraint ck_tf_block_has_hard check (
      enforcement <> 'BLOCK' or hard_limit is not null
  ),
  constraint ck_tf_overage_price check (
      (overage_enabled = false) or (overage_unit_price is not null)
  )
);

create index idx_tf_tier        on tier_feature(tier_id) where is_deleted = false;
create index idx_tf_feature     on tier_feature(feature_id) where is_deleted = false;
create index idx_tf_enabled     on tier_feature(enabled)   where is_deleted = false;
create index idx_tf_enforcement on tier_feature(enforcement) where is_deleted = false;
