# Tenant Platform Architecture

## Big ideas (guiding principles)

Bounded contexts: group code by business area (Catalog, Subscription, Billing, Policy, Tenant) rather than technology layers spread across modules.

Hexagonal (ports & adapters) inside each service: domain stays clean; infra is replaceable.

Strict dependencies: services depend on shared libraries only—never on each other’s internals.

Contracts-first: OpenAPI per service + typed clients generated/handwritten interfaces with @HttpExchange.

Observability & testability first: Testcontainers, Contract tests, Micrometer, consistent logging and error model.

One parent to rule them all: a platform BOM/parent controls versions and plugins; children don’t repeat them.

## Proposed module map

```
tenant-platform/
├── platform-bom/                  # Parent + dependency management (Spring Boot parent or BOM import)
├── platform-starters/             # Company starters (opt-in)
│   ├── starter-core/              # Common autoconfig: logging, tracing, problem-details, Jackson
│   ├── starter-data/              # JPA defaults, Flyway, testcontainers helpers
│   └── starter-openapi/           # Springdoc config, common API metadata
├── platform-libs/                 # Pure libraries (no @SpringBootApplication)
│   ├── lib-tenant-context/        # TenantContext holder + filter + propagation
│   ├── lib-common-model/          # Error payloads, common DTO primitives, validation annotations
│   └── lib-events/                # Event payload classes & serializers (Kafka/Rabbit bindings optional)
├── services/                      # Bounded contexts (independent deployables)
│   ├── catalog-service/
│   │   ├── app/                   # Spring Boot app (only wiring/boot)
│   │   ├── domain/                # Entities/aggregates, services, ports
│   │   ├── adapters/
│   │   │   ├── persistence/       # JPA repositories, mappers
│   │   │   ├── api/               # Controllers, DTOs, mappers
│   │   │   └── messaging/         # Outbox/publish domain events
│   │   └── test/                  # integration with Testcontainers
│   ├── subscription-service/
│   ├── billing-service/
│   ├── policy-service/
│   └── tenant-service/            # CRUD tenant, settings, keys, integration keys
├── apis/                          # Client SDKs and contracts shared *only* as interfaces/artifacts
│   ├── catalog-api/               # `@HttpExchange` interfaces, API DTOs (no server code)
│   ├── subscription-api/
│   ├── billing-api/
│   ├── policy-api/
│   └── tenant-api/
├── ops/                           
│   ├── docker-compose.dev.yaml    # Local dev (DBs, Kafka, Zipkin)
│   └── k8s/                       # Manifests/Helm templates per service (optional)
└── README.md
```

## Why this helps

Anyone can open `services/<name>` and see the same internal layout.

`apis/*-api` gives you typed, versioned client contracts without coupling to implementation.

`platform-*` hides repetitive config, versions, and wiring—every service stays lean.

## What goes where (quick responsibilities)

### platform-bom
Java version, Spring Boot version, plugin management (surefire/failsafe, enforcer, jacoco), version pins (lombok, springdoc, mapstruct, testcontainers), and rules (ban duplicate deps, require versions via BOM).

### platform-starters
* **starter-core**: Problem+JSON error handler, logging MDC with tenantId, request id, Micrometer registry, tracing.
* **starter-data**: Spring Data JPA defaults, Flyway location convention `db/migration/{schema}`, Testcontainers test slices.
* **starter-openapi**: Springdoc group naming, common metadata, security schemes.

### platform-libs
* **lib-tenant-context**: TenantContext (ThreadLocal), WebFilter/OncePerRequestFilter, RestClient/Feign interceptor to propagate header `X-Tenant-Id`.
* **lib-common-model**: ApiError, PageResponse, value objects, Jakarta validation utils.
* **lib-events**: Event envelopes, outbox table DDL, Jackson module for events.

### apis/*-api
`@HttpExchange` or Feign-style interfaces + DTOs used by other services. Generated clients (optional) live here if you use OpenAPI codegen.

### services/*-service
* **domain**: entities/aggregates (JPA or pure), domain services, ports (e.g. CatalogPort, OveragePort).
* **adapters**: implement ports (JPA repositories; HTTP controllers; messaging publishers/consumers).
* **app**: SpringBoot main class + minimal config; import starter-*.

### Dependency rules (enforced with maven-enforcer)
* services/* → may depend on `platform-starters`, `platform-libs`, and `apis/*-api`.
* services/* → must not depend on another service’s code.
* apis/*-api → pure interfaces & DTOs only (no @SpringBootApplication, no persistence).
* platform-* → depends on nothing in services/* or apis/*-api.

Use `maven-enforcer-plugin`:
* RequireUpperBoundDeps (avoid version conflicts)
* BanCircularDependencies
* EnforceBytecodeVersion (Java 17+)
* DependencyConvergence (if you can live with it)

### Naming & package guide (example for catalog-service)

```
com.ejada.tenant.catalog
  ├── app
  │   └── CatalogApplication.java
  ├── domain
  │   ├── model/Feature.java, ProductTier.java
  │   ├── service/EffectiveFeatureService.java
  │   └── port
  │       ├── in/  (use cases)        e.g. ResolveEffectiveFeaturesUseCase
  │       └── out/ (dependencies)     e.g. FeatureRepositoryPort, EventPublisherPort
  └── adapters
      ├── api
      │   ├── controller/CatalogController.java
      │   └── dto/FeatureDto.java, mappers (MapStruct)
      ├── persistence
      │   ├── entity/*.java
      │   ├── spring/*.java (Spring Data interfaces)
      │   └── mapper/*.java
      └── messaging
          └── OutboxPublisher.java
```

## Tenancy model (make it obvious)

* **Tenant resolution**: `X-Tenant-Id` header (fallback to subdomain if you want). Centralized in `lib-tenant-context`.
* **Persistence strategy**: choose one and document it:
  * Schema-per-tenant (Flyway per schema + `AbstractRoutingDataSource`),
  * or Row-level with `tenant_id` column + Hibernate `@Filter`.
* **Propagation**: Rest clients add the header; messaging includes tenant in event envelope.

## Inter-service communication

Prefer synchronous HTTP with `@HttpExchange` clients declared in `apis/*-api`.

For async workflows (billing overages, subscription changes), publish domain events (outbox pattern).

API versioning: `/v1/catalog/...`, `/v1/subscriptions/...`; deprecate via OpenAPI.

## Observability

Micrometer + Prometheus: enabled in starter-core.

Tracing: Micrometer Tracing (Zipkin/OTel) + HTTP + JDBC auto instrumentation.

Problem Details (RFC 9457): consistent error schema from starter-core.

Audit/Access logs: include `tenantId`, `traceId`, `userId` (if present).

## Testing strategy

* **Unit**: domain services (no Spring).
* **Slice tests**: `@DataJpaTest`, `@WebMvcTest`.
* **Integration**: `@SpringBootTest` with Testcontainers (Postgres, Kafka/Rabbit).
* **Contract tests**:
  * Provider: verify controllers match OpenAPI.
  * Consumer: verify `@HttpExchange` clients against provider stub.

## Build & CI tips

* Root `platform-bom` controls plugin versions (surefire/failsafe, spring-boot-maven-plugin, jacoco).
* `mvn -U -T 1C clean verify` in CI; publish `*-api` and `platform-libs` to your artifact repo first.
* Sonar/Jacoco thresholds on domain packages (don’t punish controllers).

## What to do with your current modules

**Current → Proposed mapping:**

* `tenant-core` → split:
  * things like JDBC and resolvers that are cross-cutting → `platform-starters` or `platform-libs`
  * business logic that belongs to Tenant → move into `tenant-service`
* `tenant-persistence` → anti-pattern as a shared persistence module. Move JPA entities & repos into each service’s adapters/persistence. Shared persistence leads to tight coupling.
* `tenant-api (server)` → rename to `tenant-service`. Keep REST server here only.
* `tenant-config` → if it’s just shared config classes, move into `starter-*`. If it’s externalized config, consider Spring Cloud Config (or keep as Helm/k8s config).
* `catalog-service`, `subscription-service`, `billing-service`, `policy-service` → keep, but adopt the uniform internal layout (`app/domain/adapters`).
* `tenant-events` → move pure event classes to `lib-events`; move any runtime consumers/producers into each service’s `adapters/messaging`.

## Concrete migration steps (order that works)

1. Create `platform-bom` and make every module inherit it (or Boot parent).
2. Add `platform-starters` and move shared config there (OpenAPI, ProblemDetails, logging, tracing).
3. Introduce `lib-tenant-context`; wire header filter + Rest client interceptor.
4. Create `apis/*-api` modules; move `@HttpExchange` interfaces & DTOs there.
5. In each service, adopt `app/domain/adapters` structure. Move JPA entities/repos from `tenant-persistence` into the owning service.
6. Replace direct inter-service dependencies with `apis/*-api` clients.
7. Add Testcontainers baseline and one integration test per service.
8. Turn on enforcer rules in `platform-bom`.
9. Remove deprecated modules (`tenant-persistence`, `tenant-events` runtime bits) after code moves.
10. Document the flow in `README.md` with a diagram.

### Example call flow (Subscription → Billing overage)

* `subscription-service` exposes `/v1/subscriptions/{id}/usage`.
* It emits `UsageExceeded` event (with `tenantId`) via outbox.
* `billing-service` consumes the event and creates an overage line; exposes `/v1/billing/overages`.
* `tenant-service` stores tenant configs used by both (via `tenant-api` client).
* All HTTP calls share `X-Tenant-Id` propagated by `lib-tenant-context`.

