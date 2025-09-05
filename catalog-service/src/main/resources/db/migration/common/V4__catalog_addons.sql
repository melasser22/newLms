-- =========================
-- ADDON (stand-alone)
-- =========================
create table IF NOT EXISTS addon (
  addon_id      bigserial primary key,
  addon_cd      varchar(64)  not null,              -- e.g. EXTRA_SEATS, ANALYTICS_PRO
  addon_en_nm   varchar(128) not null,
  addon_ar_nm   varchar(128) not null,
  description   text,
  category      varchar(64),                        -- optional grouping for UI
  is_active     boolean not null default true,
  is_deleted    boolean not null default false,
  created_at    timestamptz not null default now(),
  created_by    varchar(128),
  updated_at    timestamptz,
  updated_by    varchar(128),
  constraint uk_addon_cd unique (addon_cd)
);

create index idx_addon_active   on addon(is_active) where is_deleted = false;
create index idx_addon_category on addon(category);

-- =========================
-- ADDON_FEATURE (policy & limits per addon/feature)
-- same structure as TIER_FEATURE to keep parity
-- =========================
create table IF NOT EXISTS addon_feature (
  addon_feature_id     bigserial primary key,
  addon_id             bigint not null,
  feature_id           bigint not null,

  enabled              boolean not null default true,
  enforcement          varchar(24) not null default 'ALLOW',      -- ALLOW | BLOCK | ALLOW_WITH_OVERAGE

  soft_limit           numeric(18,3),
  hard_limit           numeric(18,3),
  limit_window         varchar(24),                               -- DAILY | MONTHLY | QUARTERLY | YEARLY | LIFETIME
  measure_unit         varchar(24),                               -- REQUESTS | ITEMS | MB | ...

  reset_cron           varchar(64),

  overage_enabled      boolean not null default false,
  overage_unit_price   numeric(18,4),
  overage_currency     varchar(3) default 'SAR',

  meta                 jsonb,
  is_deleted           boolean not null default false,
  created_at           timestamptz not null default now(),
  created_by           varchar(128),
  updated_at           timestamptz,
  updated_by           varchar(128),

  constraint uk_addon_feature unique (addon_id, feature_id),
  constraint fk_af_addon   foreign key (addon_id)   references addon(addon_id)   on delete cascade,
  constraint fk_af_feature foreign key (feature_id) references feature(feature_id) on delete cascade,

  constraint ck_af_enforcement check (enforcement in ('ALLOW','BLOCK','ALLOW_WITH_OVERAGE')),
  constraint ck_af_window      check (limit_window is null or limit_window in ('DAILY','MONTHLY','QUARTERLY','YEARLY','LIFETIME')),
  constraint ck_af_soft_hard   check (soft_limit is null or hard_limit is null or hard_limit >= soft_limit),
  constraint ck_af_block_has_hard check (enforcement <> 'BLOCK' or hard_limit is not null),
  constraint ck_af_overage_price check ((overage_enabled = false) or (overage_unit_price is not null))
);
create index idx_af_addon       on addon_feature(addon_id) where is_deleted = false;
create index idx_af_feature     on addon_feature(feature_id) where is_deleted = false;
create index idx_af_enabled     on addon_feature(enabled) where is_deleted = false;
create index idx_af_enforcement on addon_feature(enforcement) where is_deleted = false;

-- =========================
-- TIER_ADDON (which add-ons are allowed/packaged with a tier)
-- =========================
create table IF NOT EXISTS tier_addon (
  tier_addon_id  bigserial primary key,
  tier_id        bigint not null,
  addon_id       bigint not null,

  included       boolean not null default false,   -- true: bundled with tier at no extra charge
  sort_order     int not null default 0,

  -- optional list price metadata (catalog level; billing service owns actual invoicing)
  base_price     numeric(18,4),
  currency       varchar(3),

  is_deleted     boolean not null default false,
  created_at     timestamptz not null default now(),
  created_by     varchar(128),
  updated_at     timestamptz,
  updated_by     varchar(128),

  constraint uk_tier_addon unique (tier_id, addon_id),
  constraint fk_ta_tier  foreign key (tier_id)  references tier(tier_id)   on delete cascade,
  constraint fk_ta_addon foreign key (addon_id) references addon(addon_id) on delete cascade,

  constraint ck_ta_period check (billing_period is null or billing_period in ('MONTHLY','YEARLY','ONE_TIME'))
);
create index idx_ta_tier   on tier_addon(tier_id) where is_deleted = false;
create index idx_ta_addon  on tier_addon(addon_id) where is_deleted = false;
create index idx_ta_inc    on tier_addon(included) where is_deleted = false;
