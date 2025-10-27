# API Gateway Local Development Fix Guide

This guide walks through the changes required to run the API Gateway in a local Docker Compose
environment without a service registry. It consolidates the routing, discovery, and operational
updates introduced by the accompanying code changes.

## 1. Service Discovery

The gateway now uses Spring Cloud LoadBalancer's simple discovery client. Service instances are
provided in [`application-dev.yaml`](../api-gateway/src/main/resources/application-dev.yaml) with
metadata describing their management context paths.

### Actions

1. Ensure the gateway runs with the `dev` profile (already configured in `docker-compose`).
2. Confirm that each downstream service name matches the logical service ID used in the routes
   (`tenant-service`, `catalog-service`, etc.).
3. If you add a new service, append it under `spring.cloud.discovery.client.simple.instances` with
   the correct base URI and optional metadata.

## 2. Route Context Paths

Routes seeded via [`schema.sql`](../api-gateway/src/main/resources/schema.sql) now apply a
`PrefixPath` filter so that `/api/v1/**` traffic is forwarded to the service-specific context path.

### Validation Steps

```bash
# After starting the stack run:
curl -i http://localhost:8000/api/v1/tenants/healthcheck
curl -i http://localhost:8000/api/v1/catalog/plans
```

Both requests should return 200 responses (subject to seeded data). The gateway appends `/tenant`
and `/catalog` automatically after stripping the `/api/v1` prefix.

## 3. Subscription Validation & Cache Warmup

* Subscription validation is disabled in `dev` (`gateway.subscription.enabled=false`). Requests are no
  longer blocked when `subscription-service` is offline.
* Cache warmup is disabled in `dev` to avoid boot-time calls before downstream services are ready.

If you need to test these features locally, re-enable them in `application-dev.yaml` and ensure the
services are running.

## 4. Docker Compose Updates

The compose file now:

* Waits for all backend services to become **healthy** before starting the gateway.
* Uses health check URLs that respect each service's context path.
* Verifies the gateway via `http://localhost:8000/actuator/health`.

Bring the stack up using:

```bash
export JWT_SECRET=7c5b9f143e6d4a0f9b37c25d1a48f2e0
export JASYPT_ENCRYPTOR_PASSWORD=2d4a9f7c5b8e4d1c92a0f37b6c58e143

# Infrastructure tools
cd docker/tools
docker compose up -d

# Services
cd ../services
docker compose up --build
```

## 5. Verifying the Fix

Run [`verify-gateway.sh`](../verify-gateway.sh) from the repository root after the stack is up. The
script performs health checks, dumps active routes, and validates DNS resolution for the service
names used in the `lb://` URIs.

## Troubleshooting Tips

* If a service is unavailable, update the simple discovery entries to point at the correct host:port.
* Use `docker compose ps` to ensure the health checks are passing; the gateway waits for `healthy`
  status before starting.
* Check `api-gateway` logs for `Resolved service instances` entries to confirm LoadBalancer lookups.

These adjustments remove the dependency on an external service registry while maintaining consistent
routing behaviour across environments.
