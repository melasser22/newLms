# WhatsApp Management Platform

This module now defines the tenant-facing WhatsApp orchestration stack as a Maven multi-module build.
The parent service fronts tenant onboarding, configuration, and runtime operations, while child
microservices handle templates, sending, webhooks, and usage analytics.

| Module | Purpose |
| --- | --- |
| `whatsapp-management-service` | API gateway for tenant onboarding via Meta's embedded signup, storing WABA IDs and credentials in GCP Secret Manager, enforcing quotas/RBAC, and routing to child services. |
| `whatsapp-template-service` | CRUD and approval tracking for WhatsApp Highly Structured Message (HSM) templates, including attachment upload/caching and Meta submission workflows. |
| `whatsapp-sending-service` | Accepts templated or ad-hoc sends, enforces the 24-hour session window, queues jobs to Kafka, and retries with dead-letter handling. |
| `whatsapp-webhook-service` | Secure ingestion of Meta webhook events with signature/IP validation, normalization, deduplication, and Kafka event emission. |
| `whatsapp-usage-service` | Aggregates per-tenant messaging and template usage metrics, monitors quotas, and exposes reporting APIs. |

Shared concerns across the stack:

- **Persistence and caching** – PostgreSQL for tenant-scoped metadata and logs; Redis for caching template/media lookups and rate limiting.
- **Media management** – Attachment uploads cached as Meta media IDs and reused across templates and sends.
- **Security** – Tenant credentials and access tokens stored in GCP Secret Manager; RBAC enforced by the parent gateway.
- **Observability** – Kafka used for async send queues and event propagation to analytics and billing pipelines.

Build everything:

```bash
mvn -pl whatsapp-management -am clean package
```
