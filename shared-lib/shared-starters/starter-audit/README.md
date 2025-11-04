# starter-audit

Production-ready, pluggable audit logging starter for Spring Boot (Java https://json-schema.org/draft/2020-12/schema).

## Quick start
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-audit</artifactId>
  <version>1.0.0</version>
</dependency>
```

```yaml
shared:
  audit:
    enabled: true
    sinks:
      kafka:
        enabled: true
        topic: audit.events.v1
        bootstrapServers: kafka:9092
      db:
        enabled: true
```
Annotate methods:
```java
@Audited(action = AuditAction.CREATE, entity = "Customer", entityIdExpr = "#result.id")
public Customer createCustomer(CreateCustomerReq req) { ... }
```

### HTTP request auditing

The servlet filter ships with sensible defaults so that liveness endpoints (for example
`/actuator/health`) are not persisted to the audit store. Use `shared.audit.web.exclude-paths`
to override or extend the default list when integrating in your service configuration.

### Database sink configuration

When persisting audit events directly to PostgreSQL, configure the sink via
`shared.audit.sinks.db`:

```yaml
shared:
  audit:
    sinks:
      db:
        enabled: true
        schema: "security-service"
        table: audit_logs
```

* Schema names are quoted automatically, so values such as `security-service` work without
  additional escaping. If no schema is provided the starter falls back to `public`.
* The sink serializes events through a built-in redactor that masks sensitive keys in the payload
  and metadata (`password`, `accessToken`, `authorization`, `phoneNumber`, `otp`, `token`).
  This masking also applies to outbox persistence to keep retry flows safe.

For local testing, create the schema and table explicitly:

```sql
CREATE SCHEMA IF NOT EXISTS "security-service";
CREATE TABLE IF NOT EXISTS "security-service".audit_logs (
  id UUID PRIMARY KEY,
  ts_utc TIMESTAMPTZ NOT NULL,
  payload JSONB NOT NULL
);
```
