# Analytics Service

The Analytics Service provides tenant-level usage insights powered by materialized views and Redis-backed caching. It aggregates usage telemetry emitted by the Billing Service (`usage_event` table) and exposes REST APIs for cost forecasting, feature adoption, and utilization summaries.

## Features
- Refreshable materialized views for daily, monthly, and hourly usage slices
- Redis caching with a one-hour TTL to avoid repetitive aggregations
- Predictive overage detection using lightweight linear regression over recent usage
- Cost optimization recommendations based on forecasted utilization
- Tenant-scoped REST endpoints under `/api/v1/analytics/tenants/{tenantId}`

## Running locally
```
mvn spring-boot:run
```

The service expects PostgreSQL and Redis to be available (see `docker-compose.yml`).
