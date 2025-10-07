# Ejada SaaS Products Framework

The **newLms** repository hosts the multi-tenant Ejada SaaS Products Framework. It is built from
Spring Boot microservices that share a common set of starters for security, observability,
resilience, and Redis integration. An API Gateway fronts every service to provide a single edge
entry point with authentication, tenant enforcement, subscription validation, and rate limiting.

## Repository Layout

| Module | Description |
| ------ | ----------- |
| `shared-lib` | Reusable framework starters (security, core, observability, rate limiting, config) shared by all services. |
| `tenant-platform` | Aggregator Maven project for the domain services (`tenant`, `catalog`, `subscription`, `billing`, `analytics`). |
| `setup-service` | Platform bootstrap service for tenant provisioning, catalog metadata, and reference data. |
| `sec-service` | Security service responsible for IAM, audit, and policy management. |
| `api-gateway` | Spring Cloud Gateway instance that secures and routes all external traffic to the platform. |

## Runtime Topology

All external traffic flows through the gateway on port **8000**. Downstream services listen on
private container ports and are no longer exposed outside the Docker network.

| Service | Container Port | Publicly Exposed? |
| ------- | -------------- | ---------------- |
| API Gateway | 8000 | ✅ (`localhost:8000`) |
| Setup Service | 8080 | ❌ |
| Tenant Service | 8080 | ❌ |
| Catalog Service | 8080 | ❌ |
| Subscription Service | 8080 | ❌ |
| Billing Service | 8080 | ❌ |
| Analytics Service | 8080 | ❌ |
| Security Service | 8080 | ❌ |

> **Tip:** Uncomment the commented `ports` entries in
> `docker/services/docker-compose.yml` when you need to debug a service directly.

## Getting Started

### 1. Build Shared Libraries

Install the shared starters into the local Maven repository:

```bash
cd shared-lib
mvn clean install
```

### 2. Build the Platform

From the repository root you can build the entire platform, including the gateway, with:

```bash
mvn clean package
```

### 3. Start the Local Stack

The stack is now split across two compose files to give you finer control over tool dependencies
and application services.

1. Start the shared tooling (PostgreSQL, Redis, Kafka, and the OpenTelemetry collector):

   ```bash
   export JWT_SECRET=local-dev-secret
   docker compose -f docker/tools/docker-compose.yml up -d
   ```

2. Build and start the framework microservices and gateway:

   ```bash
   docker compose -f docker/services/docker-compose.yml up --build
   ```

   > **Note:** The service compose file attaches to the `lms-backend` network created by the tools
   > compose stack. Make sure the tooling stack is running before starting the services.

Once healthy, invoke the platform via the gateway:

```bash
curl http://localhost:8000/actuator/health
```

### 4. Authentication Flow

1. `POST /api/auth/login` – obtain a JWT from the security service routed via the gateway.
2. Use the issued token to call downstream APIs, e.g.:
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        http://localhost:8000/api/tenant/tenants
   ```

## Configuration Notes

- The gateway port is configurable with the `SERVER_PORT` environment variable (defaults to `8000`).
- Spring Boot lazy initialization is enabled by default across services to reduce container warm-up times; set
  `SPRING_MAIN_LAZY_INITIALIZATION=false` when eager bean creation is preferred (for example, in production load tests).
- Each microservice disables its own resource server and trusts authentication performed by the
  gateway (`shared.security.resource-server.enabled=false`).
- Redis is required for rate limiting and subscription cache validation. Configure `REDIS_HOST`
  and `REDIS_PORT` for non-Docker deployments.
- To roll back quickly, stop the `api-gateway` service and (optionally) re-enable direct ports in
  the service compose file.

## Updating Client Integrations

All client SDKs and API documentation should target the unified gateway base URL:

```
https://api.example.com/api/*
```

For example, the tenants API changed from `http://localhost:8080/tenant/api/v1/tenants` to
`http://localhost:8000/api/tenant/tenants`.

## Testing

Run unit tests for the gateway:

```bash
mvn -pl api-gateway test
```

Run a full platform build (all modules) with:

```bash
mvn clean verify
```

## Operational Checklist

- ✅ JWT validated once at the gateway.
- ✅ Tenant and subscription context propagated via gateway filters.
- ✅ Redis-backed rate limiting enabled (tenant and IP resolvers).
- ✅ Circuit breakers and fallbacks configured per route.
- ✅ Prometheus metrics exposed at `/actuator/prometheus`.

For in-depth implementation details see `docs/api-gateway-enhancement-plan.md`.
