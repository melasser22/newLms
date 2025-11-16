# email-webhook-service

Public REST surface for SendGrid webhook ingestion. The service verifies signatures, optionally
restricts requests by source IP, deduplicates payloads via Redis, and emits normalized events to
Kafka for downstream consumers.

## Endpoints

- `POST /webhooks/sendgrid` – accepts SendGrid event webhooks (array payload), validates the
  signature headers (`X-Twilio-Email-Event-Webhook-*`), and processes each event idempotently.
- `GET /admin/events` – returns processed events. Optional `type` query parameter filters by
  normalized event type.
- `GET /admin/email-logs` – returns the latest resolved email log status per SendGrid message id.
- `GET /admin/analytics` – returns counts by normalized event type for simple usage analytics.

## Configuration

Configuration lives under the `sendgrid.webhook` prefix:

| Property | Description |
| --- | --- |
| `signing-secret` | SendGrid webhook signing secret. |
| `tolerance-seconds` | Allowed age of webhook timestamps to mitigate replay attacks. |
| `allowed-ips` | Optional list of source IPs to accept. Empty list disables the check. |
| `kafka-topic` | Kafka topic to which normalized events are published. |
| `deduplication-ttl-seconds` | Redis TTL for event idempotency keys. |

The service also uses standard Spring Boot Redis (`spring.data.redis.*`) and Kafka
(`spring.kafka.bootstrap-servers`) configuration.
