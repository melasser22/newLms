# LMS API Gateway

Enterprise-ready Spring Cloud Gateway that fronts the LMS microservices. It reuses the shared LMS starters (security, observability, redis, kafka) and enforces multi-tenant policies at the edge.

## Key Capabilities

- **Security** – JWT/OIDC validation with RS256/ES256, tenant isolation, centralized CORS.
- **Resilience** – Circuit breakers, retries, and graceful fallbacks per route.
- **Performance** – Reactive rate limiting, tuned Netty client, weighted load balancing.
- **Observability** – OpenTelemetry traces, Prometheus metrics, structured JSON logs.
- **Operations** – Dynamic route reloads, blue/green routing flags, Helm deployment assets.
- **Routing Intelligence** – API version normalisation, weighted canary traffic, session affinity cookies, and operator-facing admin aggregates.
- **Tenant & Subscription Enforcement** – Dedicated filters for correlation id propagation, tenant extraction, and subscription validation with Redis caching.
- **Flexible Rate Limiting** – Built-in tenant and IP-based key resolvers that plug directly into Spring Cloud Gateway's `RequestRateLimiter` filter.
- **Downstream Client Configuration** – Centralised, load-balanced `WebClient` builder with context propagation and timeout tuning.

## Configuration Overview

All configuration lives in [`src/main/resources/application.yaml`](src/main/resources/application.yaml). Key sections include:

- `gateway.routes` – declarative downstream routes with resilience settings.
- `gateway.subscription` – subscription validation endpoint, caching, and feature mapping per route.
- `gateway.webclient` – connection and response timeout settings for the shared `WebClient` builder.
- `shared.security` – JWT/OIDC configuration and CORS policy.
- `resilience4j.*` – circuit breaker/retry/bulkhead defaults.
- `spring.cloud.gateway.*` – HTTP client tuning, rate limiting, metrics.

A configuration reload (Spring Cloud Config or Kubernetes ConfigMap) triggers the [`GatewayRoutesRefresher`](src/main/java/com/ejada/gateway/config/GatewayRoutesRefresher.java) which publishes a `RefreshRoutesEvent` and reloads dynamic routes without downtime.

## Local Development

```bash
# Run the gateway with a local profile
mvn -pl api-gateway spring-boot:run -Dspring-boot.run.profiles=local

# Execute integration tests
mvn -pl api-gateway test
```

Enable debug logging by exporting `LOGGING_LEVEL_COM_EJADA_GATEWAY=DEBUG`.

## Building the Container

```bash
# From repository root
DOCKER_BUILDKIT=1 docker build -f api-gateway/Dockerfile -t lms/api-gateway:latest .
```

The image exposes port `8080` and includes a health check that probes `/actuator/health`.

## Kubernetes Deployment

1. Provision Redis (rate limiting) and the OTEL collector.
2. Install/update the Helm release:
   ```bash
   helm upgrade --install api-gateway charts/api-gateway \
     --set image.repository=lms/api-gateway \
     --set image.tag=latest \
     --set redis.host=redis-master.lms.svc.cluster.local
   ```
3. Verify rollout:
   ```bash
   kubectl get pods -l app=api-gateway
   kubectl logs deployment/api-gateway -c api-gateway
   ```

Horizontal Pod Autoscaler (HPA) targets CPU and the custom metric `gateway.requests.active`.

## Operational Runbook

- **Diagnostics**: `GET /actuator/health`, `GET /actuator/gateway/routes`, `GET /actuator/metrics/gateway.requests`.
- **Force route refresh**: `kubectl exec` into the pod and run `curl -X POST localhost:8080/actuator/refresh`.
- **Tenant onboarding**: publish a new route via a `GatewayRouteDefinitionProvider` implementation backed by the onboarding DB; the refresher reloads automatically.
- **Blue/green deployment**: add weighted routes in config (e.g., `weight: 90` vs `10`) or leverage service mesh rules.
- **Subscription cache**: subscription lookups are cached for five minutes in Redis using the `gateway.subscription.cache-ttl` setting. Evict by deleting keys with the prefix `gateway:subscription:`.

## Testing & Quality Gates

- `ReactiveRateLimiterFilterTest` – validates rate limiting semantics.
- `GatewaySecurityConfigurationTest` – ensures JWT/OIDC wiring.
- `GatewayRoutesConfigurationTest` – checks dynamic route assembly.
- Contract tests use WireMock to emulate downstream services and Testcontainers for Redis.
- `SubscriptionValidationGatewayFilter` integrates with Testcontainers Redis to verify caching semantics (see `gateway.subscription` properties).

## Troubleshooting

| Symptom | Action |
|---------|--------|
| `401 Unauthorized` | Confirm JWK/issuer URIs and that the JWT `aud` matches configured audiences. |
| `429 Too Many Requests` | Adjust `shared.ratelimit.capacity` or tenant-specific overrides in Redis. |
| High latency | Inspect `/actuator/metrics/gateway.requests` and downstream service metrics; consider increasing circuit breaker thresholds. |

Refer to [`docs/api-gateway-enhancement-plan.md`](../docs/api-gateway-enhancement-plan.md) for the full modernization plan.
