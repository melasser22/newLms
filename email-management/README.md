# Email Management Platform

This module now acts as a Maven aggregator for the tenant-focused notification platform. Each child
service has a narrow responsibility and can be deployed independently on GKE.

| Module | Purpose |
| --- | --- |
| `email-management-service` | API gateway/orchestrator that fronts the other services and keeps tenant routing logic centralized. |
| `email-template-service` | Template CRUD/versioning and SendGrid synchronization. |
| `email-sending-service` | Idempotent email dispatch, Kafka fan-out, Redis rate limits. |
| `email-webhook-service` | Secure SendGrid webhook ingestion and event emission. |
| `email-usage-service` | Queryable usage analytics backed by Cloud SQL. |

Build everything:

```bash
mvn -pl email-management -am clean package
```
