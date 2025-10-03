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
| shared-lib (parent) | Spring Boot starters and common DTOs consumed by the services below. |

## Build
```bash
mvn -T4 clean verify
```

## Run locally
From the project root, run:
```bash
docker-compose up --build
```
All business services run on the internal Docker network; the only public entry point is the [API Gateway](../api-gateway) on `http://localhost:8088`.
