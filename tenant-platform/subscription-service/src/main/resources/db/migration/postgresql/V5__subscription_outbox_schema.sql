create schema if not exists subscription;

do $$
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = 'public'
          and table_name = 'outbox_event'
    ) then
        execute 'alter table public.outbox_event set schema subscription';
    end if;
end $$;

create table if not exists subscription.outbox_event (
  id             bigserial primary key,
  aggregate_type varchar(64) not null,
  aggregate_id   varchar(128) not null,
  event_type     varchar(64) not null,
  payload        jsonb       not null,
  headers        jsonb,
  created_at     timestamptz not null default now(),
  processed_at   timestamptz
);

create index if not exists idx_outbox_unprocessed
  on subscription.outbox_event(aggregate_type, aggregate_id)
  where processed_at is null;
