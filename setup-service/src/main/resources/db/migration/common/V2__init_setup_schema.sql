create schema if not exists setup;

create table if not exists setup.country (
  country_id serial primary key,
  country_cd varchar(3) not null,
  country_en_nm varchar(256) not null,
  country_ar_nm varchar(256) not null,
  dialing_code varchar(10),
  nationality_en varchar(256),
  nationality_ar varchar(256),
  is_active boolean not null default true,
  en_description varchar(1000),
  ar_description varchar(1000),
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  constraint uq_country_cd unique (country_cd)
);
create index if not exists idx_country_cd on setup.country(country_cd);
create index if not exists idx_country_en_nm on setup.country(country_en_nm);

create table if not exists setup.city (
  city_id serial primary key,
  city_cd varchar(50) not null,
  city_en_nm varchar(200) not null,
  city_ar_nm varchar(200) not null,
  country_id integer not null references setup.country(country_id),
  is_active boolean not null default true,
  constraint uk_city_cd unique(city_cd)
);
create index if not exists idx_city_country_active on setup.city(country_id,is_active);
create index if not exists idx_city_en_nm on setup.city(city_en_nm);
create index if not exists idx_city_ar_nm on setup.city(city_ar_nm);

create table if not exists setup.lookup (
  lookup_item_id integer primary key,
  lookup_item_cd varchar(255),
  lookup_item_en_nm varchar(255),
  lookup_item_ar_nm varchar(255),
  lookup_group_code varchar(255),
  parent_lookup_id varchar(255),
  is_active boolean,
  item_en_description varchar(1000),
  item_ar_description varchar(1000)
);

create table if not exists setup.system_parameter (
  param_id serial primary key,
  param_key varchar(150) not null unique,
  param_value varchar(1000) not null,
  description varchar(1000),
  group_code varchar(150),
  is_active boolean not null default true
);

create table if not exists setup.resources (
  resource_id serial primary key,
  resource_cd varchar(128) not null,
  resource_en_nm varchar(256) not null,
  resource_ar_nm varchar(256) not null,
  path varchar(512),
  http_method varchar(16),
  parent_resource_id integer,
  is_active boolean not null default true,
  en_description varchar(1000),
  ar_description varchar(1000),
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz,
  constraint uq_resource_cd unique(resource_cd)
);
create index if not exists idx_resource_cd on setup.resources(resource_cd);
create index if not exists idx_resource_parent on setup.resources(parent_resource_id);

insert into setup.system_parameter (param_key, param_value, description, group_code, is_active)
values ('MAX_USERS','1000','Maximum allowed users','SYSTEM', true)
on conflict (param_key) do nothing;
