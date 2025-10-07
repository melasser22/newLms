# Gateway Request Flow (Dev Profile)

This document summarises the lifecycle of a typical request passing through the gateway with the
dev-profile fixes applied.

1. **Client Request** – A caller hits `GET /api/v1/tenants/{tenantId}` on port `8000`.
2. **Route Matching** – Spring Cloud Gateway matches the `/api/v1/tenants/**` predicate seeded from
   [`schema.sql`](../api-gateway/src/main/resources/schema.sql).
3. **Filter Chain**
   * `StripPrefix=1` removes `/api/v1` from the path.
   * `PrefixPath=/tenant` reintroduces the downstream service context path.
   * Additional filters (e.g. request headers) execute as defined in the route metadata.
4. **Subscription Validation** – Disabled in the `dev` profile (`gateway.subscription.enabled=false`),
   so the request proceeds without calling `subscription-service`.
5. **Load Balancing**
   * The gateway resolves `lb://tenant-service` using the simple discovery entries defined in
     [`application-dev.yaml`](../api-gateway/src/main/resources/application-dev.yaml).
   * `CompositeLoadBalancer` evaluates available instances; with simple discovery there will be a
     single Docker container per service.
6. **Downstream Call** – The configured `WebClient` (exposed by
   [`GatewayWebClientConfiguration`](../api-gateway/src/main/java/com/ejada/gateway/config/GatewayWebClientConfiguration.java))
   issues the HTTP request to `http://tenant-service:8080/tenant/...`.
7. **Response Handling** – The response propagates back through any filters and returns to the client.

For catalog routes replace `/tenant` with `/catalog`; other services follow the same pattern as long
as the `PrefixPath` filter aligns with their servlet context path.

## Warmup & Background Tasks

* Cache warmup is inactive in `dev`, preventing background requests before services report healthy
  status.
* The `verify-gateway.sh` script exercises the actuator health and route listings once the stack is
  running, providing a quick confidence check after boot.
