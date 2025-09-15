-- *****************************************************************************
-- Billing DB bootstrap (PostgreSQL)
-- - Usage tracking (counters + immutable events)
-- - Invoicing (invoice + lines)
-- - Attachments (PDFs/CSVs stored inline; you can switch to external storage later)
-- *****************************************************************************

-- =========================
--  EXTENSION(S)
-- =========================
-- (jsonb is native; uuid-ossp not required if app supplies UUIDs)

-- =========================
--  USAGE / CONSUMPTION
-- =========================
create table if not exists usage_counter (
  usage_counter_id      bigserial primary key,
  ext_subscription_id   bigint      not null,
  ext_customer_id       bigint      not null,
  consumption_typ_cd    varchar(32) not null
    check (consumption_typ_cd in ('TRANSACTION','USER','BALANCE')),
  -- for TRANSACTION and USER types
  current_consumption   bigint,
  -- for BALANCE type
  current_consumed_amt  numeric(18,4),
  updated_at            timestamptz not null default now(),
  unique (ext_subscription_id, consumption_typ_cd)
);
comment on table usage_counter is 'Current usage snapshot per subscription & type';
comment on column usage_counter.consumption_typ_cd is 'TRANSACTION | USER | BALANCE (swagger enums)';

create table if not exists usage_event (
  usage_event_id        bigserial primary key,
  rq_uid                uuid        not null,
  token_hash            varchar(64),
  payload               jsonb       not null,
  ext_product_id        bigint      not null,
  received_at           timestamptz not null default now(),
  processed             boolean     not null default true,
  status_code           varchar(32) not null,
  status_desc           varchar(128) not null,
  status_dtls           jsonb
);
comment on table usage_event is 'Immutable audit of Track Product Consumption requests & outcomes';

-- =========================
--  INVOICING (MINIMAL)
-- =========================
create table if not exists invoice (
  invoice_id            bigserial primary key,
  ext_subscription_id   bigint      not null,
  ext_customer_id       bigint      not null,
  currency              varchar(3)  not null,
  subtotal_amt          numeric(18,4) not null default 0,
  tax_amt               numeric(18,4) not null default 0,
  total_amt             numeric(18,4) not null default 0,
  invoice_dt            date        not null,
  due_dt                date,
  status_cd             varchar(32) not null default 'DRAFT', -- DRAFT|ISSUED|PAID|VOID
  created_at            timestamptz not null default now()
);
comment on table invoice is 'Commercial invoice header for a subscription/customer';

create table if not exists invoice_item (
  invoice_item_id       bigserial primary key,
  invoice_id            bigint      not null references invoice(invoice_id) on delete cascade,
  line_no               int         not null,
  item_cd               varchar(64) not null, -- FEATURE/ADDON/USAGE/etc.
  item_desc             varchar(256),
  qty                   numeric(18,4) not null default 1,
  unit_price            numeric(18,6) not null default 0,
  line_total            numeric(18,4) not null default 0
);
comment on table invoice_item is 'Invoice line items (rated usage, recurring charges, add-ons)';

-- =========================
--  ATTACHMENTS
-- =========================
create table if not exists invoice_attachment (
  invoice_attachment_id bigserial primary key,
  invoice_id            bigint not null references invoice(invoice_id) on delete cascade,
  file_nm               varchar(255) not null,
  mime_typ              varchar(128) not null,
  content               bytea        not null,
  created_at            timestamptz  not null default now()
);
comment on table invoice_attachment is 'Inline binary attachments for invoices (e.g., PDF)';

-- =========================
--  OPTIONAL: OUTBOX (for async integrations)
-- =========================
create table if not exists outbox_event (
  outbox_event_id       bigserial primary key,
  aggregate_type        varchar(64) not null,
  aggregate_id          varchar(128) not null,
  event_type            varchar(64) not null,
  payload               jsonb       not null,
  created_at            timestamptz not null default now(),
  published             boolean     not null default false,
  published_at          timestamptz
);
comment on table outbox_event is 'Transactional outbox to integrate billing with other services';

-- =========================
--  COMMENTARY / DOCS
-- =========================
-- Track endpoint contract driven by "Track Product Consumption" swagger
-- (/subscription/product-consumption/track) — enums & shapes mirrored above.
-- See swagger: ServiceResult«TrackProductConsumptionRs», ProductConsumption(consumptionTypCd), etc.
-- (we will validate request/response at controller level)
-- Source: marketplace Swagger for track product consumption. :contentReference[oaicite:0]{index=0}
