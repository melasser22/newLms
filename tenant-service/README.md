# Entitlement Service

Java 21 / Spring Boot 3.5.2 microservice for multi-tenant feature entitlements and overage recording.

## Features
- PostgreSQL with Flyway migrations
- Redis caching, Kafka messaging
- Multi-tenant support via Postgres RLS and request filter
- Overage policy enforcement and recording
- REST API with OpenAPI docs at `/swagger-ui`
- Docker and Docker Compose
- GitHub Actions CI

## Running locally
```
./mvnw -f entitlement-service/pom.xml spring-boot:run
```
Or using docker-compose:
```
docker-compose up --build
```
