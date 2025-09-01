# Tenant Platform

Combined platform containing tenant management services and LMS platform microservices (policy, tenant, subscription, catalog, billing).

## Build
```bash
mvn -T4 clean verify
```

## Run locally
```bash
docker-compose up --build
```
Services listen on `localhost:8081`..`8084`.
