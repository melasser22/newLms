# tenant-events (com.lms.tenant.events)

Transactional Outbox + Publisher for multi-tenant services.

- **Outbox table** holds events until published
- **Publisher** uses SKIP LOCKED to fetch batches safely across replicas
- **KafkaPublisher** (preferred, via shared/spring Kafka)
- **LogPublisher** fallback when Kafka not available
- **Tenant headers** sourced from MDC or `TenantContext` if on classpath

## Flyway migration (add to your service)
```sql
-- Vx__outbox.sql
do $$ begin
  if not exists (select 1 from information_schema.tables where table_name='outbox_event') then
    create table outbox_event (
      event_id uuid primary key,
      event_type text not null,
      aggregate_type text not null,
      aggregate_id uuid not null,
      occurred_at timestamptz not null default now(),
      payload jsonb not null,
      headers jsonb not null,
      status text not null default 'NEW',
      attempts int not null default 0,
      published_at timestamptz
    );
    create index ix_outbox_status_occurred on outbox_event(status, occurred_at);
  end if;
end $$;
```

## Configuration

```yaml
lms:
  events:
    topicPrefix: lms.tenant
    batchSize: 200
    maxAttempts: 10
    schedule: PT1S
    cleanupAfterDays: 14
```

## Usage

* Inject `OutboxService` and call `append(event, headers)` inside the same transaction as your domain change.
* The scheduled `OutboxPublisher` publishes to Kafka (or logs) and marks events as `PUBLISHED`.

**Note:** Replace topic naming / partitioning to your conventions. For dead-lettering, configure Kafka DLQ or extend the publisher.

```
