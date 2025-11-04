# Ejada SaaS Products Framework

The **newLms** repository hosts the multi-tenant Ejada SaaS Products Framework. It is built from
Spring Boot microservices that share a common set of starters for security, observability,
resilience, and Redis integration. Each service now exposes its HTTP API directly for
local development and testing.

## Repository Layout

| Module | Description |
| ------ | ----------- |
| `shared-lib` | Reusable framework starters (security, core, observability, rate limiting, config) shared by all services. |
| `tenant-platform` | Aggregator Maven project for the domain services (`tenant`, `catalog`, `subscription`, `billing`, `analytics`). |
| `setup-service` | Platform bootstrap service for tenant provisioning, catalog metadata, and reference data. |
| `sec-service` | Security service responsible for IAM, audit, and policy management. |

## Runtime Topology

Each microservice is reachable from the host using a unique port mapping. The table below lists
the default bindings defined in `docker/services/docker-compose.yml`.

| Service | Container Port | Publicly Exposed? |
| ------- | -------------- | ---------------- |
| Tenant Service | 8080 | ✅ (`localhost:8081`) |
| Setup Service | 8080 | ✅ (`localhost:8082`) |
| Billing Service | 8080 | ✅ (`localhost:8083`) |
| Analytics Service | 8080 | ✅ (`localhost:8084`) |
| Catalog Service | 8080 | ✅ (`localhost:8085`) |
| Subscription Service | 8080 | ✅ (`localhost:8086`) |
| Security Service | 8080 | ✅ (`localhost:8087`) |

## Getting Started

### 1. Build Shared Libraries

Install the shared starters into the local Maven repository:

```bash
cd shared-lib
mvn clean install
```

### 2. Build the Platform

From the repository root you can build the entire platform with:

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

2. Build and start the framework microservices:

   ```bash
   docker compose -f docker/services/docker-compose.yml up --build
   ```

   > **Note:** Both compose stacks share the `lms-backend` network. The services compose file now
   > creates the network automatically when it is missing, but you should still start the tooling
   > stack first so PostgreSQL, Redis, Kafka, and the OpenTelemetry collector are ready when the
   > services boot.

Once healthy, you can invoke service health checks directly. For example, to verify the tenant
service is running:

```bash
curl http://localhost:8081/actuator/health
```

### 4. Authentication Flow

1. `POST /api/auth/login` – obtain a JWT from the security service (`http://localhost:8087`).
2. Use the issued token to call downstream APIs directly on each service, e.g.:
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
        http://localhost:8081/api/tenant/tenants
   ```

### 5. Security Service Authorization Requirements

The superadmin management APIs exposed by the security service (`/api/v1/auth/admins/**`) are
guarded by Spring Security method expressions and require:

- A JWT that includes the `ROLE_EJADA_OFFICER` authority.
- An `X-Tenant-Id` header when tenant verification is enabled (default in development profiles).

You can verify access with the following example request:

```bash
curl -X GET "http://localhost:8087/api/v1/auth/admins?page=0&size=20" \
  -H "Authorization: Bearer $EJADA_OFFICER_JWT" \
  -H "X-Tenant-Id: tenant-1" \
  -H "Accept: application/json"
```

A token missing `ROLE_EJADA_OFFICER` (or omitting the tenant header when required) will
receive a `403 Forbidden` response.

## Configuration Notes

- Spring Boot lazy initialization is enabled by default across services to reduce container warm-up times; set
  `SPRING_MAIN_LAZY_INITIALIZATION=false` when eager bean creation is preferred (for example, in production load tests).
- Each microservice runs its own resource server (`shared.security.resource-server.enabled=true`)
  and validates JWTs on inbound requests.
- Redis is required for rate limiting and subscription cache validation. Configure `REDIS_HOST`
  and `REDIS_PORT` for non-Docker deployments.

## Testing

Run a full platform build (all modules) with:

```bash
mvn clean verify
```
