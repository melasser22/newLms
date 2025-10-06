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
