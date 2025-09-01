# tenant-config (shared module)

Reusable Spring Boot module providing multi-tenant plumbing:

- `TenantContext` (ThreadLocal)
- `TenantResolverService` (slug/domain → tenant UUID)
- `TenantResolverFilter` (extracts tenant, sets MDC + Postgres `app.current_tenant`)
- `TenantAwareDataSource` + BeanPostProcessor (ensures every borrowed connection runs `set_config`)
- Auto-configuration via Spring Boot imports; configurable with properties

## Install / Use
Add dependency to your service `pom.xml`:
```xml
<dependency>
  <groupId>com.lms.tenant</groupId>
  <artifactId>tenant-config</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Ensure your service has Postgres + Flyway with a `tenant` table (id, slug, domains\[], status enum).

## Configuration (application.yaml)

```yaml
lms:
  tenant:
    resolution:
      use-subdomain: true              # parse subdomain as tenant key
      header-primary: X-Tenant-ID      # fallback header
      header-secondary: X-Auth-Tenant  # secondary fallback
      wrap-data-source: true           # wrap DataSource to set app.current_tenant per connection
```

## Notes

* The filter sets `MDC[tenant_id]` for log correlation.
* `TenantAwareDataSource` avoids leaks when multiple connections are used in a single request.
* If your shared-library provides a resolver API, you can replace `TenantResolverService` via a bean override.

```
