# Spring Boot Reactive Microservice Startup Optimization Plan

## 1. Optimized `application.yml` Snippet
```yaml
spring:
  main:
    lazy-initialization: true
    allow-bean-definition-overriding: false
  lifecycle:
    timeout-per-shutdown-phase: 20s
  data:
    r2dbc:
      url: r2dbc:postgresql://postgres:5432/app
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      pool:
        enabled: true
        initial-size: 0
        max-size: 40
        max-idle-time: 45s
        max-create-connection-time: 5s
        max-life-time: 15m
        validation-query: SELECT 1
        validation-depth: remote
        acquire-retry: 2
        max-pending-requests: 256
  sql:
    init:
      mode: never
      continue-on-error: false

management:
  endpoints:
    enabled-by-default: false
    web:
      exposure:
        include: ["health", "info"]
  endpoint:
    health:
      show-details: never
  metrics:
    export:
      simple:
        enabled: false
      prometheus:
        enabled: false

server:
  shutdown: graceful

logging:
  level:
    root: INFO
    org.springframework: INFO
    reactor.netty: WARN

app:
  routes:
    preload-on-startup: false
  cache:
    warmup-delay: 5s
```

### Notes
* Enables global lazy initialization for non-critical beans while allowing opt-in eager beans via `@Lazy(false)`.
* Disables automatic SQL schema initialization (moved to explicit deferred job).
* Minimizes actuator and metrics footprint to speed up startup; only essential endpoints enabled.
* Sets INFO logging baseline and quieter Netty logs.
* Configures R2DBC pool with smaller initial footprint and tuned lifecycle limits to avoid connection storms during cold start.

## 2. Container JVM Options
```
-XX:+UseZGC \
-XX:+ZGenerational \
-XX:InitialRAMPercentage=40 \
-XX:MaxRAMPercentage=65 \
-XX:MinRAMPercentage=25 \
-XX:MetaspaceSize=128m \
-XX:MaxMetaspaceSize=256m \
-XX:+AlwaysPreTouch \
-XX:+UseStringDeduplication \
-XX:+UnlockDiagnosticVMOptions \
-XX:+PrintFlagsFinal \
-XX:+EnableDynamicAgentLoading=false \
-XX:ActiveProcessorCount=4 \
-Dspring.context.exit=onRefresh \
-Dspring.main.cloud-platform=kubernetes \
-Dspring.output.ansi.enabled=ALWAYS \
-Dreactor.netty.ioWorkerCount=4 \
-Dreactor.netty.pool.maxConnections=500 \
-Dlogging.level.root=INFO
```

### Notes
* ZGC with generational mode in Java 21 offers short pauses ideal for reactive workloads.
* `AlwaysPreTouch` helps avoid first-request page faults when CDS is used.
* Limit worker threads to control CPU thrashing and align with container cores.
* `spring.context.exit` ensures graceful shutdown for AOT/CDS images.

## 3. Spring Configuration & Code Changes
* Annotate heavy, non-critical beans with `@Lazy` or configure via `@Bean @Lazy`. Keep critical gateway beans eager.
* Introduce `@Configuration(proxyBeanMethods = false)` on configuration classes to reduce CGLIB overhead.
* Use `@ImportRuntimeHints` for reflection hints when enabling AOT.
* Move dynamic route repository initialization into `ApplicationRunner` annotated bean and execute via `@Async`.
* For caches: create `SmartLifecycle` bean with `isAutoStartup() == false` and trigger from readiness callback using `ApplicationAvailability`.
* Apply `@ConditionalOnProperty` for optional integrations (e.g., tracing, messaging) to avoid startup scanning when disabled.
* Enable native hints for Netty + R2DBC when generating GraalVM native image or CDS archive.

## 4. Best Practices: DB Init, Route Loading, Cache Warmup
1. **Deferred Schema Initialization**
   * Disable `spring.sql.init`. Provide Flyway/Liquibase migration or custom `SchemaInitializer` scheduled via `ApplicationReadyEvent`.
   * Use `ConnectionFactoryUtils.getConnection` within reactive transaction and execute `schema.sql` only when schema version mismatch is detected.
2. **Reactive Route Loading**
   * Implement `RouteLocator` bean that defers DB access until after readiness. Use `Flux.defer` + `subscribeOn(Schedulers.boundedElastic())` to avoid blocking event loop.
   * Cache the resulting route definitions in `Sinks.One` and reuse across gateway filters.
3. **Async Cache Warmup**
   * On `ApplicationReadyEvent`, submit warmup tasks to `TaskExecutor` or `Scheduler`. Guard with `@Profile("prod")` to avoid local delays.
   * Use `Mono.delay(app.cache.warmup-delay)` before warmup to ensure service is READY for probes.
   * Leverage `ApplicationAvailability` to only start warmup after `ReadinessState` is `ACCEPTING_TRAFFIC`.

## 5. Actuator & Micrometer Tuning
* Disable unneeded actuator endpoints; keep `health` & `info` only.
* Configure `management.metrics.enable.*` to `false` except required counters.
* Use `MeterFilter` to deny expensive metrics (e.g., `http.server.requests` if not scraped).
* If Prometheus needed, expose via sidecar after startup using `management.metrics.export.prometheus.pushgateway.enabled=true` with `shutdownOperation: PUSH` to defer registry creation.
* Set `management.health.probes.enabled=true` and rely on `@Readiness` events rather than full health checks at boot.

## 6. Startup Timeline (Expected)
| Stage | Current (s) | Target (s) | Actions |
| --- | --- | --- | --- |
| JVM boot + classpath scan | 40 | 15 | Lazy init, config optimizations, CDS |
| Spring context refresh | 120 | 45 | Deferred bean creation, disable actuator/OpenAPI, limit auto-config |
| DB schema init | 35 | 5 | Deferred async schema check & apply |
| Dynamic route loading | 45 | 20 | Async loading post-ready with caching |
| Cache warmup | 10 | 5 | Off-main-thread warmup |
| **Total** | **250** | **90** | **~64% reduction** |

## Implementation Checklist
- [ ] Enable Spring Boot 3.4+ AOT (`mvn -Pnative spring-boot:process-aot`) and generate CDS layer during CI/CD build.
- [ ] Build layered JAR (`spring-boot.build-image` or Docker `layers=true`) to reuse base layers and speed up cold starts.
- [ ] Add startup profiling (`--debug --spring.main.log-startup-info=true`) in lower environments to monitor bean timing.
- [ ] Monitor connection pool metrics after tuning to ensure no starvation during traffic spikes.
