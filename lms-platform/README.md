# LMS Platform

Multi-service Spring Boot platform (Java 21, Boot 3.5.2) with tenant, subscription, catalog and billing services.

## Build
```bash
mvn -T4 clean verify
```

## Run locally
```bash
docker-compose up --build
```
Services listen on `localhost:8081`..`8084`.
