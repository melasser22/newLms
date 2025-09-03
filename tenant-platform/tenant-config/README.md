# tenant-config

Spring Boot starter providing request-scoped tenant resolution and PostgreSQL GUC propagation.

## Configuration

Add typical datasource properties to enable PostgreSQL:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/app
    username: user
    password: pass
```
```
