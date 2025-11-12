-- ========= Inbound audit + idempotency keyed by rqUID =========
create table IF NOT EXISTS inbound_notification_audit (
  inbound_notification_audit_id bigserial primary key,
  rq_uid          uuid        not null,            -- header rqUID
  token_hash      varchar(128),                    -- optional: hash the token for traceability
  endpoint        varchar(64) not null,            -- RECEIVE_NOTIFICATION | RECEIVE_UPDATE
  payload         jsonb       not null,            -- full request body as received
  received_at     timestamptz not null default now(),
  processed       boolean     not null default false,
  processed_at    timestamptz,
  status_code     varchar(16),                     -- I000000 | EINT000 (as per ServiceResult)
  status_desc     varchar(64),
  status_dtls     jsonb,
  constraint ux_inb_rquid unique (rq_uid, endpoint)
);

-- ========= Update events (receiveSubscriptionUpdate) =========
create table IF NOT EXISTS subscription_update_event (
  subscription_update_event_id bigserial primary key,
  rq_uid         uuid        not null,
  ext_subscription_id bigint not null,             -- body.subscriptionId
  ext_customer_id    bigint not null,              -- body.customerId
  update_type    varchar(16) not null,             -- SUSPENDED | RESUMED | TERMINATED | EXPIRED
  received_at    timestamptz not null default now(),
  processed      boolean     not null default false,
  processed_at   timestamptz,
  constraint ck_su_type check (update_type in ('SUSPENDED','RESUMED','TERMINATED','EXPIRED')),
  constraint ux_su_unique unique (rq_uid)
);

create index if not exists idx_sue_sub on subscription_update_event(ext_subscription_id);
