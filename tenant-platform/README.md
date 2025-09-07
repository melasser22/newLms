# Tenant Platform

Combined platform containing shared tenant components and Ejada microservices.

## Structure

The platform is composed of several Spring Boot services that share common
configuration and starters from the [`shared-lib`](../shared-lib) project.
Each service follows a standard Maven layout and provides an
`application-dev.yaml` for local development.

## Services
| Service | Description |
|---------|-------------|
| [tenant-service](tenant-service/README.md) | Tenant onboarding, domains, overage toggle. |
| [catalog-service](catalog-service/README.md) | Plans, features, limits and tenant overrides. |
| [subscription-service](subscription-service/README.md) | Tracks active subscription and billing period. |
| [billing-service](billing-service/README.md) | Usage and overage tracking with export feeds. |
| [admin-api-gateway](tenant-api/README.md) | Thin façade aggregating read models for the Admin UI. |
| [policy-service](policy-service/README.md) | Evaluates access policies across tenant modules. |

## Build
```bash
mvn -T4 clean verify
```

## Run locally
From the project root, run:
```bash
docker-compose up --build
```
Services listen on `localhost:8081`..`8084`.
