# shared-starters

Collection of Spring Boot starters that provide common infrastructure for LMS services.

## Available starters
- **starter-actuator** – opinionated actuator endpoints.
- **starter-audit** – pluggable audit logging.
- **starter-core** – core web and logging infrastructure.
- **starter-crypto** – crypto service auto-configuration.
- **starter-data** – JPA helpers and multi-tenant support.
- **starter-headers** – standardize HTTP headers.
- **starter-health** – generic health endpoints.
- **starter-kafka** – Kafka producers/consumers with idempotency.
- **starter-mapstruct** – shared MapStruct configuration and mappers.
- **starter-money-time** – money and time helpers.
- **starter-observability** – logging and metrics defaults.
- **starter-openapi** – Springdoc OpenAPI wiring.
- **starter-ratelimit** – bucket-based rate limiting filter.
- **starter-redis** – Redis connection, cache and session support.
- **starter-resilience** – Resilience4j defaults.
- **starter-security** – JWT resource server defaults.
- **starter-validation** – extra Bean Validation constraints.

## Usage
Add the desired starter as a dependency. For example:

```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-core</artifactId>
</dependency>
```
