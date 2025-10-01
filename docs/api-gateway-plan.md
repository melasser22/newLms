# API Gateway Codebase Assessment & Plan

## Phase 1 – Shared Library Inventory

| Capability | Component(s) | Location | Notes |
| --- | --- | --- | --- |
| Security & JWT | `SecurityAutoConfiguration`, `JwtAuthenticationConverter`, `JwtDecoder`, tenant propagation via `JwtTenantFilter` | `shared-lib/shared-starters/starter-security` | Provides resource-server defaults, claim-to-authority mapping, pluggable CSRF/CORS handling, and JWT decoder options (HS256/JWKS/issuer) suitable for reuse in the gateway. 【F:shared-lib/shared-starters/starter-security/src/main/java/com/ejada/starter_security/SecurityAutoConfiguration.java†L59-L199】 |
| JWT token services | `JwtTokenService`, `JwtTokenAutoConfiguration` | `shared-lib/shared-lib-crypto` | Offers HMAC-based token generation with configurable TTL for service-to-service calls or admin flows. 【F:shared-lib/shared-lib-crypto/src/main/java/com/ejada/crypto/JwtTokenService.java†L13-L92】 |
| Exception handling | `GlobalExceptionHandler`, `CoreExceptionAutoConfiguration` | `shared-lib/shared-starters/starter-core` | Centralizes REST error responses using `BaseResponse` wrappers and shared error codes. Gateway should extend to produce reactive responses. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/web/GlobalExceptionHandler.java†L29-L185】 |
| Logging | `LoggingAutoConfiguration` | `shared-lib/shared-starters/starter-core` | Configures Logback console with MDC propagation and optional JSON encoder tied to correlation/tenant IDs. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/logging/LoggingAutoConfiguration.java†L17-L58】 |
| Request context propagation | `CoreAutoConfiguration`, `ContextFilter`, `CorrelationContextContributor`, `TenantContextContributor` | `shared-lib/shared-starters/starter-core` | Handles correlation ID generation, tenant resolution, and filter registration for servlet stack. Requires reactive equivalents for WebFlux. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/config/CoreAutoConfiguration.java†L44-L200】【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/context/ContextFilter.java†L20-L118】【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/context/CorrelationContextContributor.java†L15-L74】 |
| DTOs & response wrappers | `BaseResponse`, `ErrorResponse`, request metadata models | `shared-lib/shared-common/dto` | Standardizes API envelopes; gateway responses should continue using them. 【F:shared-lib/shared-common/src/main/java/com/ejada/common/dto/BaseResponse.java†L14-L137】 |
| Common exceptions | `BusinessException`, `ValidationException`, etc. | `shared-lib/shared-common/exception` | Shared hierarchy underpinning GlobalExceptionHandler. |
| Events & messaging | Tenant provisioning & subscription approval records | `shared-lib/shared-common/events/*` | Shared Kafka payload contracts referenced by catalog/subscription listeners; gateway can reuse for orchestration or pass-through. 【F:shared-lib/shared-common/src/main/java/com/ejada/common/events/provisioning/TenantProvisioningMessage.java†L7-L18】【F:shared-lib/shared-common/src/main/java/com/ejada/common/events/subscription/SubscriptionApprovalMessage.java†L6-L25】 |
| Rate limiting | `RateLimitAutoConfiguration`, `RateLimitFilter`, `RateLimitProps` | `shared-lib/shared-starters/starter-ratelimit` | Servlet filter backed by Redis; behavior needs adaptation for WebFlux. 【F:shared-lib/shared-starters/starter-ratelimit/src/main/java/com/ejada/shared_starter_ratelimit/RateLimitAutoConfiguration.java†L1-L16】【F:shared-lib/shared-starters/starter-ratelimit/src/main/java/com/ejada/shared_starter_ratelimit/RateLimitFilter.java†L1-L27】【F:shared-lib/shared-starters/starter-ratelimit/src/main/java/com/ejada/shared_starter_ratelimit/RateLimitProps.java†L8-L24】 |
| Configuration baseline | `AppProperties`, `CentralConfigAutoConfiguration` | `shared-lib/shared-config` | Provides centralized `app.*` properties that downstream services import. 【F:shared-lib/shared-config/src/main/java/com/ejada/config/CentralConfigAutoConfiguration.java†L6-L13】【F:shared-lib/shared-config/src/main/java/com/ejada/config/AppProperties.java†L6-L17】 |

**Usage patterns & compatibility**

* All starters target servlet-based Spring Boot (Spring MVC) applications, relying on servlet filters. Adapting for Spring Cloud Gateway (reactive stack) will require wrapper components for filters and exception handlers.
* DTOs, error codes, and logging rely on shared enums/contexts; ensure the gateway exposes the same MDC keys (correlationId, tenantId) and response formats to remain compatible.
* Redis, Kafka, and security starters assume Spring Boot 3 / Java 21 (matching microservices), so version compatibility is aligned for gateway implementation.

## Phase 2 – Microservice Architectural Patterns

| Area | Observations | Evidence |
| --- | --- | --- |
| Authentication & authorization | REST controllers return `BaseResponse` envelopes and delegate to shared security starter; endpoints require JWT with role enforcement. | `AuthController` endpoints use shared DTOs and responses. 【F:sec-service/src/main/java/com/ejada/sec/controller/AuthController.java†L16-L59】 Configuration enables shared security props. 【F:sec-service/src/main/resources/application.yaml†L29-L56】 |
| Service communication | Predominantly RESTful; Kafka used for cross-service orchestration (tenant provisioning, subscription approvals). No Feign clients detected. | Billing controller handles synchronous REST requests. 【F:tenant-platform/billing-service/src/main/java/com/ejada/billing/controller/ConsumptionController.java†L20-L45】 Kafka listeners consume shared events. 【F:tenant-platform/catalog-service/src/main/java/com/ejada/catalog/kafka/TenantProvisioningListener.java†L13-L29】 |
| Circuit breaking & resilience | Route definitions now use Spring Cloud CircuitBreaker with Resilience4j and per-route fallbacks; services can additionally opt in via shared starter-resilience. |
| Service discovery | No evidence of Eureka/Consul annotations; services appear to run behind static routing (likely via API gateway/load balancer). |
| Configuration management | Services import `shared-config` and define layered `application-*.yaml` profiles for environment overrides. 【F:sec-service/src/main/resources/application.yaml†L1-L61】 |
| Logging & tracing | Use SLF4J loggers combined with shared correlation/tenant context from starters. 【F:setup-service/src/main/java/com/ejada/setup/service/impl/LookupServiceImpl.java†L29-L125】 |
| Error handling | Controllers return `BaseResponse` via service layer; shared `GlobalExceptionHandler` ensures consistent codes for validation/data errors. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/web/GlobalExceptionHandler.java†L51-L185】 |
| API versioning | No explicit versioning; controllers mount under `/api/...` prefixes, version likely handled via routing or header conventions. |
| Database transactions | Extensive use of `@Transactional` for service methods, mixing read-only and default propagation. 【F:setup-service/src/main/java/com/ejada/setup/service/impl/LookupServiceImpl.java†L29-L125】 |
| Caching strategies | Redis-backed caching via `@Cacheable` with explicit cache names (lookups, addons, etc.). 【F:setup-service/src/main/java/com/ejada/setup/service/impl/LookupServiceImpl.java†L43-L85】 |
| Observability & headers | Shared security headers and actuator SLA reporting enabled through configuration. 【F:sec-service/src/main/resources/application.yaml†L29-L56】 |

## Phase 3 – Gateway Integration Opportunities

1. **Direct reuse**
   * Security auto-configuration & shared security properties for JWT validation and role mapping. 【F:shared-lib/shared-starters/starter-security/src/main/java/com/ejada/starter_security/SecurityAutoConfiguration.java†L59-L199】
   * Global error DTOs and error codes via `BaseResponse` and shared exceptions. 【F:shared-lib/shared-common/src/main/java/com/ejada/common/dto/BaseResponse.java†L14-L137】
   * Logging conventions (Logback encoder, MDC keys) and configuration properties to keep logs consistent. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/logging/LoggingAutoConfiguration.java†L17-L58】
   * Shared event payloads so the gateway can forward or publish Kafka messages without redefining schemas. 【F:shared-lib/shared-common/src/main/java/com/ejada/common/events/provisioning/TenantProvisioningMessage.java†L7-L18】

2. **Adaptation needs**
   * Servlet-based filters (`ContextFilter`, `RateLimitFilter`, `JwtTenantFilter`) require reactive equivalents for `GatewayFilter` / `WebFilter`. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/context/ContextFilter.java†L20-L118】【F:shared-lib/shared-starters/starter-ratelimit/src/main/java/com/ejada/shared_starter_ratelimit/RateLimitFilter.java†L1-L27】
   * `GlobalExceptionHandler` should be wrapped by a Spring Cloud Gateway `ErrorWebExceptionHandler` to emit reactive `BaseResponse`. 【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/web/GlobalExceptionHandler.java†L51-L185】
   * Rate limiting uses `StringRedisTemplate` (blocking); gateway will need `ReactiveStringRedisTemplate` or reactive script execution.

3. **Gaps to address**
   * Gateway-specific routing configuration (Spring Cloud Gateway `RouteLocator`) and predicate definitions are absent.
   * No reactive authentication manager or token relay in shared library; gateway needs to bridge shared JWT decoder into reactive security.
   * Aggregation/fallback logic (e.g., resilience, circuit breaking) is not implemented in existing services; gateway should introduce Resilience4j Reactor operators if required.

4. **Potential conflicts**
   * Mixing servlet-based starters inside a reactive gateway may autoconfigure incompatible beans (e.g., `FilterRegistrationBean`). Need to conditionally disable servlet-only auto-configurations when running on WebFlux.
   * Rate limiting semantics rely on tenant context from servlet `ContextManager`; reactive context propagation must align to avoid mismatched keys.

## Phase 4 – API Gateway Implementation Plan

### ✅ Components to REUSE from shared-lib
- [ ] `SecurityAutoConfiguration` beans (reuse JWT decoder, role conversion, and shared security props)【F:shared-lib/shared-starters/starter-security/src/main/java/com/ejada/starter_security/SecurityAutoConfiguration.java†L59-L199】
- [ ] `JwtTokenService` for administrative token issuance if needed【F:shared-lib/shared-lib-crypto/src/main/java/com/ejada/crypto/JwtTokenService.java†L13-L92】
- [ ] `GlobalExceptionHandler` error codes & DTOs (wrap for WebFlux responses)【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/web/GlobalExceptionHandler.java†L51-L185】
- [ ] `LoggingAutoConfiguration` MDC strategy (ensure MDC population via Reactor context)【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/logging/LoggingAutoConfiguration.java†L17-L58】
- [ ] `BaseResponse` / shared DTOs for consistent payloads【F:shared-lib/shared-common/src/main/java/com/ejada/common/dto/BaseResponse.java†L14-L137】
- [ ] Kafka event records for pass-through messaging【F:shared-lib/shared-common/src/main/java/com/ejada/common/events/provisioning/TenantProvisioningMessage.java†L7-L18】

### 🔧 Components to ADAPT
- [ ] Wrap servlet `ContextFilter` & contributors into reactive `WebFilter` to maintain correlation/tenant IDs via Reactor context and `ServerWebExchange`【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/context/ContextFilter.java†L20-L118】
- [ ] Re-implement `RateLimitFilter` as `GatewayFilter` using `ReactiveStringRedisTemplate` while honoring existing configuration properties【F:shared-lib/shared-starters/starter-ratelimit/src/main/java/com/ejada/shared_starter_ratelimit/RateLimitFilter.java†L1-L27】【F:shared-lib/shared-starters/starter-ratelimit/src/main/java/com/ejada/shared_starter_ratelimit/RateLimitProps.java†L8-L24】
- [ ] Translate `GlobalExceptionHandler` mappings into a reactive `ErrorAttributes`/`ErrorWebExceptionHandler` pipeline that still emits `BaseResponse` structures【F:shared-lib/shared-starters/starter-core/src/main/java/com/ejada/starter_core/web/GlobalExceptionHandler.java†L51-L185】
- [ ] Bridge shared JWT decoder into `ReactiveAuthenticationManager` for Spring Security WebFlux, preserving claim-to-authority conversion【F:shared-lib/shared-starters/starter-security/src/main/java/com/ejada/starter_security/SecurityAutoConfiguration.java†L84-L163】

### ⭐ Components to CREATE NEW
- [ ] Spring Cloud Gateway `RouteLocator` definitions for microservice routing, including path-based predicates and load-balanced URIs
- [ ] Gateway-level aggregation/forwarding handlers for endpoints that combine multiple downstream calls (if required)
- [ ] Resilience policies (timeouts, retries, circuit breakers) using Resilience4j Reactor operators tailored to critical routes
- [x] Expose declarative retry/backoff controls and customizable fallback messaging through gateway route configuration
- [ ] Reactive tenant & correlation context propagators integrated with Reactor context to keep MDC/logging consistent
- [ ] API documentation (OpenAPI) describing gateway endpoints referencing shared DTOs

### ⚠️ Components to AVOID duplicating
- [ ] Do **not** reimplement JWT parsing/validation; rely on shared decoder and token service
- [ ] Do **not** redefine error codes or DTOs; use `BaseResponse` and shared enums
- [ ] Do **not** introduce alternative logging formats; reuse shared MDC conventions
- [ ] Do **not** create new Kafka payload models; reuse shared event records

## Phase 5 – Next Steps & Validation Checklist

1. **Dependency setup**: Add `spring-cloud-starter-gateway` alongside existing shared starters; ensure servlet-only starters are excluded or conditioned when running on WebFlux.
2. **Reactive adapters**: Implement adapters for context propagation, rate limiting, and exception handling before adding new functionality.
3. **Route configuration**: Define route predicates and filters (auth, rate limit, logging) referencing shared configuration properties.
4. **Security hardening**: Integrate shared JWT decoder, align CORS/security headers with microservices, and ensure tenant/role checks align with downstream expectations.
5. **Testing**: Create contract tests validating error envelopes, correlation header echo, and rate limiting counters against Redis.
6. **Deployment alignment**: Mirror microservice `application.yaml` structure for environment-specific settings (profiles, shared.* namespaces) to ease operations consistency.【F:sec-service/src/main/resources/application.yaml†L1-L61】

## Phase 6 – Validation Checklist for Implementation

- [ ] Confirm every gateway utility is backed by a shared component or adapter.
- [ ] Verify reactive filters populate MDC and propagate tenant/correlation IDs.
- [ ] Ensure error responses remain `BaseResponse` with shared error codes.
- [ ] Validate rate limiting honors tenant/user/IP strategies defined in `RateLimitProps`.
- [ ] Align gateway logging/output with `LoggingAutoConfiguration` expectations.
- [ ] Reuse shared configuration namespaces (`shared.security`, `shared.core`, `shared.ratelimit`, etc.) for consistency across services.
