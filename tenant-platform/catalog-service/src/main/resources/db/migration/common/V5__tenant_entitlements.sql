-- Tenant-specific feature and addon entitlements provisioned via Kafka
create table if not exists tenant_feature_entitlement (
    tenant_feature_entitlement_id serial primary key,
    tenant_code                  varchar(64)  not null,
    feature_cd                   varchar(128) not null,
    feature_count                integer,
    created_at                   timestamptz not null default now(),
    updated_at                   timestamptz,
    unique (tenant_code, feature_cd)
);

create table if not exists tenant_addon_entitlement (
    tenant_addon_entitlement_id serial primary key,
    tenant_code                 varchar(64)  not null,
    addon_cd                    varchar(128) not null,
    product_additional_service_id bigint,
    service_name_en             varchar(256),
    service_name_ar             varchar(256),
    service_price               numeric(18,4),
    total_amount                numeric(18,4),
    currency                    varchar(3),
    is_countable                boolean,
    requested_count             bigint,
    payment_type_cd             varchar(32),
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz,
    unique (tenant_code, addon_cd)
);
