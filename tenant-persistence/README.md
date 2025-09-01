# tenant-persistence (com.lms.tenant.persistence)

Central JPA entities + repositories + Flyway migrations for the tenant domain.

## Add to a service
```xml
<dependency>
  <groupId>com.lms.tenant</groupId>
  <artifactId>tenant-persistence</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Ensure your service config includes:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
```

## Notes

* PostgreSQL **RLS** is enabled/forced on `tenant_integration_key`; the service should set `app.current_tenant` (see tenant-config module).
* Arrays are mapped via Hibernate 6 `@JdbcTypeCode(SqlTypes.ARRAY)` to a `text[]` column.
* DDL is **idempotent** and safe for multiple deployments.

