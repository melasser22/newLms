# Multi-tenant Push Notification Architecture

## Overview
This document outlines a parent-child microservice topology for a Firebase-backed push notification subsystem built on Spring Boot, Kafka, Redis (Memorystore), PostgreSQL, and Google Cloud Platform (GCP). The design enforces tenant isolation, quota controls, and scalable asynchronous delivery while integrating with Firebase Cloud Messaging (FCM).

## Services and Responsibilities
### Parent: `push-management`
- **API gateway**: Exposes REST/GraphQL endpoints for template CRUD, device registration, ad-hoc and template-based sends, previews, and usage reports.
- **Tenant context**: Validates JWTs, resolves `tenantId`, and propagates it via headers (e.g., `X-Tenant-Id`) and Kafka message attributes.
- **Configuration registry**: Stores tenant Firebase project metadata (project id, sender id), credential references (Secret Manager resource ids), default topics, and quota plans.
- **Rate limiting & quotas**: Enforces per-tenant QPS and daily/monthly send limits using Redis token buckets; rejects or defers requests exceeding limits.
- **Orchestration**: Routes CRUD operations to child services, fan-outs send requests, and aggregates status responses. Handles idempotency keys for client retries.
- **Audit & observability**: Emits structured audit events to Kafka and metrics to Cloud Monitoring with tenant labels.
- **Tenancy guardrails**: Every inbound request must include a `tenantId` claim in the JWT. The gateway injects `tenantId`, `requestId`, and `idempotencyKey` into downstream calls and Kafka headers. A missing or mismatched `tenantId` results in `403` to avoid cross-tenant data access.

### Child: `push-template-service`
- CRUD for tenant-scoped templates with versioning and localization (title/body/image/custom data schema).
- Validates placeholders and payload size against Firebase limits; supports preview rendering with sample variables.
- Warm caches active templates and latest versions in Redis; publishes cache-invalidations on template changes.

### Child: `push-sending-service`
- Accepts send requests (template id+variables or ad-hoc payload) via Kafka from `push-management`.
- Validates device tokens (known/unknown) and resolves template content via Redis fallback to REST.
- Produces jobs onto `push.send` topic; consumer workers batch messages and invoke Firebase Admin SDK.
- Implements retry with exponential backoff, circuit breaking for provider errors, and DLQ routing for exhausted jobs.
- Persists send attempts and delivery receipts to PostgreSQL for traceability.
- **Firebase project mapping**: Chooses the Firebase project and service account per tenant. Service account json is fetched from Secret Manager and cached in-memory with TTL, isolated per tenant.

### Child: `push-device-management-service`
- Manages device tokens per tenant/user: registration, refresh, invalidation, and cleanup of stale tokens based on FCM feedback.
- Supports topic/group subscription management and enforces anti-abuse and per-user limits via Redis.
- Publishes device lifecycle events (register/refresh/revoke) to Kafka for template personalization and usage analytics.

### Child: `push-usage-service`
- Consumes send/receipt/device events to aggregate daily/monthly metrics per tenant and template.
- Tracks deliveries, failures, opens/engagement events, and quota consumption; triggers alerts when thresholds near limits.
- Exposes reporting APIs and supports billing/export integration.
- **Quota enforcement feedback loop**: Maintains per-tenant counters in Redis and Postgres. When limits are hit, publishes alerts and signals `push-management` to throttle subsequent requests.

## API Surface (gateway)
- **Template CRUD**: `POST /templates`, `PUT /templates/{id}`, `GET /templates/{id}/versions`, `POST /templates/{id}/preview` (body: variables map).
- **Device lifecycle**: `POST /devices/register`, `POST /devices/{token}/refresh`, `DELETE /devices/{token}`, `POST /devices/{token}/topics/{topic}`.
- **Send**: `POST /send` with `{templateId?, variables?, adHocPayload?, tokens[], topic?, idempotencyKey}`. Returns a `requestId` used for status polling (`GET /send/{requestId}`) and webhook callbacks.
- **Usage**: `GET /usage/daily`, `GET /usage/monthly`, `GET /usage/templates/{id}` with tenant-scoped filters.

HTTP responses include `X-Request-Id`, `X-Tenant-Id`, and `Idempotency-Key` headers. All APIs are OpenAPI-described and require `Authorization: Bearer <jwt>`.

## Data Model (PostgreSQL)
All tables include `tenant_id` and Row-Level Security (RLS) policies:
- **Tenant config**: `push_tenant_settings` (firebase_project_id, firebase_credential_ref, default_topic, plan, daily_limit, monthly_limit, rate_limit_qps).
- **Templates**: `push_templates` (id, tenant_id, key, status), `push_template_versions` (template_id, version, locale, title, body, image_url, data_json, is_active, created_by).
- **Devices**: `push_devices` (id, tenant_id, user_id, device_token, platform, app_id, status, last_seen_at), `push_device_topics` (device_id, topic, subscribed_at).
- **Sends & receipts**: `push_send_requests` (idempotency_key, request_source, payload_ref, status), `push_send_logs` (send_id, device_token, template_version_id, status, error_code, firebase_message_id, attempt_no, sent_at, delivered_at, opened_at).
- **Usage**: `push_usage_daily` and `push_usage_monthly` (template_id, counts per status, engagement metrics), `push_alerts` for threshold violations.

Indexes prioritize tenant isolation and performance: `(tenant_id, key)` on templates, `(tenant_id, device_token)` unique on devices, `(tenant_id, idempotency_key)` unique on send requests, and partitioning on usage tables by day/month. Database constraints ensure payload sizes respect Firebase limits before enqueueing.

## Kafka Topics & Contracts
- `push.request` – requests from `push-management` to `push-sending-service` (headers: tenantId, idempotencyKey, priority, ttlSeconds).
- `push.send` – internal fan-out for worker batches; messages include resolved payload and target tokens.
- `push.send.dlq` – failures exceeding retries or validation errors.
- `push.device.events` – device register/refresh/revoke events from device service.
- `push.usage` – send outcomes and engagement events emitted by sending service.

Messages embed tenant metadata and correlation ids; schemas are versioned with JSON Schema/Avro stored in Schema Registry.

**Message contracts (examples)**
- `push.request` value: `{ requestId, tenantId, templateId?, locale?, variables?, adHocPayload?, target: { tokens[], topic? }, priority?, ttlSeconds, idempotencyKey }`
- `push.send` value: `{ batchId, tenantId, firebaseProjectId, payload: { title, body, imageUrl?, data }, targets: tokens[], dryRun? }`
- `push.usage` value: `{ eventId, tenantId, requestId, token, templateVersionId?, status, errorCode?, firebaseMessageId?, occurredAt }`

## Redis Usage (Memorystore)
- Template cache: `{tenant}:{templateKey}:{locale}:activeVersion` → rendered payload envelope; invalidated on template updates.
- Rate limiting: token buckets per tenant (`rate:{tenant}:qps`) and per-IP/device registration (`reg:{tenant}:{ip}`).
- Send deduplication: idempotency key hashes for `push_send_requests` to short-circuit duplicates.
- Device token shadow state: recent validity checks to avoid repeated FCM invalid tokens.

Template cache TTL is configurable (e.g., 5–15 minutes) and includes a version hash to avoid serving stale content. Rate-limit buckets are prefixed with deployment environment to separate staging vs. prod.

## Request Flows
### Template lifecycle
1. Client calls `push-management` to create/update a template; JWT provides tenant context.
2. Parent validates quota for templates, persists via `push-template-service`, and clears Redis caches.
3. Template previews render via `push-template-service` using stored schema and sample variables.

### Device registration
1. App registers/refreshes token through `push-management`; requests are rate limited.
2. Parent delegates to `push-device-management-service`, which stores/upserts token and publishes `push.device.events`.
3. Invalidations from FCM feedback mark tokens inactive and propagate cache updates.

### Sending notifications
1. Client submits a send request (template id+variables or ad-hoc) to `push-management` with idempotency key.
2. Parent enforces quotas/QPS, resolves template if provided, and publishes a normalized request to `push.request`.
3. `push-sending-service` validates tokens (known/unknown), resolves payload (Redis → template service), and enqueues to `push.send` for batching.
4. Worker consumes batches, calls Firebase Admin SDK, records attempts, and emits outcomes to `push.usage` (success/failure/open).
5. `push-usage-service` aggregates metrics, updates quota consumption, and triggers alerts when thresholds near limits.

## Security, Secrets, and Isolation
- JWT validation with tenant claims; `tenant_id` propagated via headers, Kafka headers, and database RLS.
- Firebase credentials stored per tenant in GCP Secret Manager; `push-sending-service` fetches and caches short-lived service account tokens.
- TLS for all ingress; mutual TLS/authorized service accounts for inter-service calls on GKE.
- Background jobs remove or anonymize stale data per data retention policies.

All database connections use `SET app.current_tenant` at session start; RLS policies verify `tenant_id = current_setting('app.current_tenant')`. Kafka consumers also apply a tenant filter before processing to avoid cross-tenant leakage.

## Deployment & Observability
- Deploy services to GKE; Cloud SQL for PostgreSQL; Memorystore for Redis; Kafka (or Pub/Sub equivalent) for messaging.
- Helm charts include autoscaling settings, health probes, and ConfigConnector/Workload Identity bindings for Secret Manager access.
- Expose OpenTelemetry traces/metrics/logs with tenant labels; dashboards for send throughput, errors, latency, quota usage, and device churn.

SLOs: 99th percentile end-to-end send latency < 2s for single notifications; < 5% error rate over 5-minute windows per tenant. Alerts fire on Kafka lag, Redis saturation, Firebase error spikes, and quota exhaustion.

## Error Handling & Resilience
- Retry policies tuned separately for transient Firebase errors vs. permanent failures; DLQ consumers trigger alerts and optional manual replay.
- Circuit breakers around Firebase calls with fallback to queue delay; backpressure via Kafka consumer lag monitoring.
- Idempotent processing using idempotency keys, send request hashes, and unique constraints in `push_send_requests`.
- Bulk sends chunk targets into batches (e.g., 500 tokens) to honor FCM limits. Per-tenant worker concurrency is capped to prevent noisy-neighbor behavior.
