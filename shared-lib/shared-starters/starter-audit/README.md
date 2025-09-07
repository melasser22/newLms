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
        bootstrapServers: localhost:29092
      db:
        enabled: true
```
Annotate methods:
```java
@Audited(action = AuditAction.CREATE, entity = "Customer", entityIdExpr = "#result.id")
public Customer createCustomer(CreateCustomerReq req) { ... }
```
