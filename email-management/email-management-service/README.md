# email-management-service

Gateway facade that exposes a consolidated API surface for tenants while routing work to the
specialized child services (template, sending, webhook, and usage). The service discovers the child
base URLs via `child-services.*` properties and forwards requests using Spring's `RestClient`.

The parent microservice also centralizes platform-wide capabilities:

- **Tenant authz/authn** enforced through the `TenantAuthenticationFilter`, propagated via
  `TenantContextHolder`, and validated with per-tenant tokens.
- **Unified API gateway** controllers fan out into the appropriate downstream service and aggregate
  the response for both REST and lightweight GraphQL clients.
- **Global configuration and feature flags** are provided by `GlobalConfigService` so portal requests
  can toggle optional capabilities per tenant.
- **Centralized audit logging and monitoring** is captured through the `AuditLogger` helper that logs
  every inbound request/action with the tenant id.
- **Workflow orchestration** happens in `TenantExperienceService`, which blends template, usage, and
  configuration data into UI-ready payloads.
- **Rate limiting** is enforced at the API boundary via `TenantRateLimiter`, preventing noisy tenants
  from exhausting resources.
