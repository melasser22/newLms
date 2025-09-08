# WebFlux Migration Notes

This repository has begun migration from the blocking Spring MVC stack to the reactive Spring WebFlux stack.

## Key updates
- Replaced `spring-boot-starter-web` with `spring-boot-starter-webflux` and added `reactor-netty`.
- Switched data access from JPA/JDBC to R2DBC with the PostgreSQL reactive driver.
- Retained Flyway for schema migrations using the JDBC driver.
- Added reactor-kafka for non-blocking Kafka integration and reactive testing utilities.
- Configured R2DBC and Flyway settings in application configuration.

These changes lay the groundwork for fully reactive controllers, services and messaging pipelines across the project.
