# Push Notification Platform

This Maven module aggregates the Firebase-oriented push notification services described in the
architecture guide. Each child service keeps a single responsibility so the stack can scale and
be deployed independently.

| Module | Purpose |
| --- | --- |
| `push-management-service` | Gateway/orchestrator that fronts the child services and forwards tenant-scoped requests. |
| `push-template-service` | In-memory template CRUD, preview, and locale-specific versioning. |
| `push-sending-service` | Accepts send requests, logs them per tenant, and optionally emits Kafka jobs. |
| `push-device-management-service` | Registers, refreshes, and revokes device tokens per tenant. |
| `push-usage-service` | Tracks aggregated send/delivery/open counters per tenant. |

Build everything:

```bash
mvn -pl push-notification -am clean package
```
