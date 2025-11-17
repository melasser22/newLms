# SMS Management Platform

A multi-tenant SMS platform built on Spring Boot, Kafka, Redis, and PostgreSQL running on GCP. The design centers on a parent **sms-management** service that fronts specialized child services. CEQUENS is the upstream SMS provider, and all tenant-facing APIs are exposed through the parent service.

## Modules
| Module | Purpose |
| --- | --- |
| `sms-management-service` | Gateway that fronts the SMS stack and exposes tenant-friendly entrypoints. |
| `sms-template-service` | Manages template CRUD/versioning and keeps hot templates cached for senders. |
| `sms-sending-service` | Accepts send requests, enforces rate limits/idempotency, and publishes envelopes to Kafka. |
| `sms-webhook-service` | Receives CEQUENS delivery/reply callbacks and deduplicates inbound events. |
| `sms-usage-service` | Surfaces recent usage windows to tenants and dashboards. |

Build everything:

```bash
mvn -pl sms-management -am clean package
```

## Goals
- Strong tenant isolation with end-to-end `tenantId` propagation.
- First-class support for template-based and ad-hoc SMS dispatch.
- Elastic throughput with Kafka-backed fan-out, retries, and dead-letter handling.
- Fast reads through Redis caches and rate-limit counters.
- Observable, auditable delivery pipelines with webhook reconciliation.

## Service Topology
- **sms-management (parent/gateway)**
  - Authenticates and authorizes tenant and admin requests.
  - Stores tenant-wide CEQUENS credentials, callback secrets, and per-tenant quotas.
  - Enforces rate limits and usage quotas (Redis token buckets keyed by `tenantId`).
  - Routes REST API calls to child services and aggregates responses.
  - Issues signing secrets for outbound CEQUENS webhook validation.
  - Exposes unified APIs for template CRUD, sending, and usage reporting.

- **sms-template-service**
  - CRUD for templates scoped by `tenantId` with versioning and locale variants.
  - Validates dynamic variables and renders previews for admin approval.
  - Syncs active versions to CEQUENS where required or serves internal renderings.
  - Caches active templates and compiled variable metadata in Redis.

- **sms-sending-service**
  - Accepts single/bulk send requests referencing templates or free-form bodies.
  - Validates payloads/recipients (allowing unknown recipients) and merges per-template defaults (e.g., tags/metadata).
  - Publishes send intents to Kafka for async processing; workers call CEQUENS with tenant credentials.
  - Implements retry/backoff and a dedicated DLQ topic; ensures idempotency per `tenantId` + client reference.
  - Persists message requests, states, and CEQUENS responses in PostgreSQL for auditing.

- **sms-webhook-service**
  - Receives CEQUENS delivery/reply callbacks on tenant-specific endpoints.
  - Verifies signatures, timestamps, and provider IP ranges; applies idempotency keys to dedupe.
  - Normalizes events and updates message status logs in PostgreSQL.
  - Emits normalized delivery/reply events to Kafka for downstream consumers.

- **sms-usage-service**
  - Aggregates usage per tenant/template (successes, failures, retries, billable segments).
  - Evaluates quotas and threshold alerts; signals violations to `sms-management` and Kafka.
  - Exposes APIs for usage, billing exports, and alert configurations.

## Data Model Highlights (PostgreSQL)
- `tenants`: CEQUENS credentials, webhook secrets, quotas, rate-limit configs.
- `templates`: template metadata, tenant ownership, default metadata/attachments.
- `template_versions`: versioned content with locale, variable schema, activation flags.
- `messages`: send requests, rendered bodies, template references, client reference IDs.
- `message_events`: status transitions (queued/sent/delivered/failed/replied) with provider payloads.
- `usage_rollups`: aggregated counts per tenant/template/day plus quota state.

All tables include `tenant_id` and are indexed for lookups by `tenant_id`, status, and created time. Flyway migrations live with each service.

## Kafka Topics
- `sms.send.request`: accepted sends from `sms-sending-service` (single/bulk fan-out).
- `sms.send.ready`: worker-ready messages after template rendering/validation.
- `sms.send.dlq`: unprocessable or exhausted retry messages.
- `sms.events.delivery`: normalized delivery/reply events from `sms-webhook-service`.
- `sms.usage.metrics`: emit counters for `sms-usage-service` aggregation.

Each topic is partitioned by `tenantId` to keep ordering per tenant and simplify rate enforcement.

## Redis Usage
- Template cache keyed by `tenantId:templateCode:version:locale` with TTL-backed invalidation on updates.
- Rate-limit counters/token buckets per tenant and per sender ID.
- Idempotency keys for send requests and webhook events.
- Short-term message state cache for quick status lookups during heavy load.

## API Surface (via sms-management)
- **Templates**: `POST /tenants/{tenantId}/templates`, `GET /.../{templateCode}`, version activation, localization CRUD, preview rendering.
- **Send**: `POST /tenants/{tenantId}/sms` (ad-hoc), `POST /tenants/{tenantId}/sms/by-template` (template-based, supports bulk recipients), optional client reference for idempotency.
- **Usage**: `GET /tenants/{tenantId}/usage` with filters (template, date range, status), `GET /tenants/{tenantId}/billing/exports`.
- **Admin**: quota/rate-limit updates, CEQUENS credential rotation, webhook secret rotation.

Child services expose internal APIs/async handlers; only `sms-management` is internet-facing. All requests carry `X-Tenant-Id` propagated through Feign/RestTemplate interceptors and Kafka headers.

## Sending Flow
1. Request hits `sms-management`, authenticated/authorized; rate/usage checks run via Redis + quota state.
2. For template sends, `sms-management` calls `sms-template-service` to fetch/render the active version and variable schema validation.
3. Payload is forwarded to `sms-sending-service`, which validates recipients and enqueues to `sms.send.request` with idempotency keys.
4. Workers consume, call CEQUENS with tenant-scoped credentials, and write message + provider response to PostgreSQL. Retries use exponential backoff; terminal failures land in `sms.send.dlq`.
5. `sms-sending-service` emits to `sms.events.delivery` once an initial provider status is known.

## Webhook Flow
1. CEQUENS calls `sms-webhook-service` endpoint dedicated per tenant (e.g., `/webhook/{tenantId}`).
2. Service verifies signature/IP and checks Redis for replayed event IDs.
3. Events are normalized, logged to `message_events`, and published to `sms.events.delivery` for other services.
4. `sms-sending-service` reconciles statuses; `sms-usage-service` updates counters/alerts.

## Usage & Quota Flow
- `sms-usage-service` consumes `sms.events.delivery` and `sms.usage.metrics`, rolling up per tenant/template.
- Quota thresholds trigger alerts to `sms-management`, which can throttle further sends or notify admins.
- APIs expose summarized metrics for dashboards and billing exports.

## Security & Observability
- JWT-based tenant auth; service-to-service auth via mTLS and signed Kafka messages.
- Secrets (CEQUENS tokens, webhook secrets) stored in Secret Manager; rotated via `sms-management` admin APIs.
- OpenTelemetry for traces/metrics/logs; logs include `tenantId` and `clientRef` for correlation.
- Audit logging of admin actions (credential/quota changes).

## Deployment (GCP)
- GKE for all services, backed by Cloud SQL for PostgreSQL.
- Google Memorystore for Redis; Pub/Sub-compatible Kafka (e.g., Confluent Cloud) with private networking.
- Cloud Load Balancer terminates TLS in front of `sms-management` and `sms-webhook-service`.
- Cloud Armor/IP allowlists for webhook ingress; Cloud Scheduler for quota resets and template cache sweeps.

This design keeps multi-tenant concerns centralized while letting each service remain independently scalable and deployable.
