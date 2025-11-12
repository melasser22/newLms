-- ========= Core Subscription (from subscriptionInfo) =========
create table IF NOT EXISTS subscription (
  subscription_id       bigserial primary key,
  ext_subscription_id   bigint       not null,     -- swagger: subscriptionInfo.subscriptionId (long)
  ext_customer_id       bigint       not null,     -- swagger: subscriptionInfo.customerId (long)
  ext_product_id        bigint       not null,     -- swagger: subscriptionInfo.productId (long)
  ext_tier_id           bigint       not null,     -- swagger: subscriptionInfo.tierId (long)
  tier_nm_en            varchar(256),
  tier_nm_ar            varchar(256),

  start_dt              date         not null,     -- swagger: startDt (YYYY-MM-DD)
  end_dt                date         not null,     -- swagger: endDt   (YYYY-MM-DD)

  subscription_amount   numeric(18,4),
  total_billed_amount   numeric(18,4),
  total_paid_amount     numeric(18,4),

  subscription_stts_cd  varchar(8)   not null,     -- swagger: subscriptionSttsCd (string)
  create_channel        varchar(32),               -- PORTAL | GCP_MARKETPLACE

  unlimited_users_flag  varchar(1)   default 'N',  -- Y/N
  users_limit           bigint,
  users_limit_reset_type varchar(32),              -- FULL_SUBSCRIPTION_PERIOD | PAYMENT_FREQUENCY_PERIOD

  unlimited_trans_flag  varchar(1)   default 'N',  -- Y/N
  transactions_limit    bigint,
  trans_limit_reset_type varchar(32),              -- FULL_SUBSCRIPTION_PERIOD | PAYMENT_FREQUENCY_PERIOD

  balance_limit         numeric(18,4),
  balance_limit_reset_type varchar(32),            -- FULL_SUBSCRIPTION_PERIOD | PAYMENT_FREQUENCY_PERIOD

  environment_size_cd   varchar(8),                -- L | XL

  is_auto_prov_enabled  varchar(1)   default 'N',  -- Y/N
  prev_subscription_id  bigint,
  prev_subscription_update_action varchar(16),     -- UPGRADE | DOWNGRADE | RENEWAL

  meta                  jsonb,
  is_deleted            boolean      not null default false,
  created_at            timestamptz  not null default now(),
  created_by            varchar(128),
  updated_at            timestamptz,
  updated_by            varchar(128),

  constraint ux_sub_unique_ext unique (ext_subscription_id, ext_customer_id)
);

create index IF NOT EXISTS idx_sub_ext_customer on subscription(ext_customer_id) where is_deleted=false;
create index IF NOT EXISTS idx_sub_ext_product  on subscription(ext_product_id)  where is_deleted=false;
create index IF NOT EXISTS idx_sub_status_cd    on subscription(subscription_stts_cd) where is_deleted=false;

-- ========= Subscription Features (from subscriptionInfo.SubscriptionFeatureLst[]) =========
create table IF NOT EXISTS subscription_feature (
  subscription_feature_id bigserial primary key,
  subscription_id         bigint not null,
  feature_cd              varchar(128) not null,   -- swagger: featureCd (string)
  feature_count           integer,                 -- swagger: featureCount (int32)
  created_at              timestamptz not null default now(),
  updated_at              timestamptz,
  constraint fk_sf_sub foreign key (subscription_id) references subscription(subscription_id) on delete cascade,
  constraint ux_sf unique (subscription_id, feature_cd)
);
create index IF NOT EXISTS idx_sf_sub on subscription_feature(subscription_id);

-- ========= Subscription Additional Services (from subscriptionAdditionalServicesLst[]) =========
create table IF NOT EXISTS subscription_additional_service (
  subscription_additional_service_id bigserial primary key,
  subscription_id         bigint not null,
  product_additional_service_id bigint not null,   -- swagger: productAdditionalServiceId (long)
  service_cd              varchar(128) not null,   -- swagger: serviceCd
  service_name_en         varchar(256) not null,
  service_name_ar         varchar(256) not null,
  service_desc_en         text,
  service_desc_ar         text,

  service_price           numeric(18,4),           -- swagger: servicePrice (double)
  total_amount            numeric(18,4),
  currency                varchar(3),

  is_countable            varchar(1) default 'N',  -- Y/N
  requested_count         bigint,

  payment_type_cd         varchar(32),             -- ONE_TIME_FEES | WITH_INSTALLMENT

  created_at              timestamptz not null default now(),
  updated_at              timestamptz,
  constraint fk_sas_sub foreign key (subscription_id) references subscription(subscription_id) on delete cascade
);
create index IF NOT EXISTS idx_sas_sub on subscription_additional_service(subscription_id);

-- ========= Product Properties (from productProperties[] at root of notification request) =========
create table IF NOT EXISTS subscription_product_property (
  subscription_product_property_id bigserial primary key,
  subscription_id   bigint not null,
  property_cd       varchar(128) not null,
  property_value    varchar(2048) not null,
  created_at        timestamptz not null default now(),
  constraint fk_spp_sub foreign key (subscription_id) references subscription(subscription_id) on delete cascade,
  constraint ux_spp unique (subscription_id, property_cd)
);
create index IF NOT EXISTS idx_spp_sub on subscription_product_property(subscription_id);

-- ========= Provisioning Environment Identifiers we return (ReceiveSubscriptionNotificationRs.environmentIdentiferLst) =========
create table IF NOT EXISTS subscription_environment_identifier (
  subscription_env_id bigserial primary key,
  subscription_id     bigint not null,
  identifier_cd       varchar(64) not null,        -- e.g. DB_ID
  identifier_value    varchar(512) not null,       -- e.g. "10.20.0.1"
  created_at          timestamptz not null default now(),
  constraint fk_sei_sub foreign key (subscription_id) references subscription(subscription_id) on delete cascade,
  constraint ux_sei unique (subscription_id, identifier_cd)
);
create index IF NOT EXISTS idx_sei_sub on subscription_environment_identifier(subscription_id);
