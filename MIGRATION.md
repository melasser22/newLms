# Spring WebFlux Migration

This repository has been updated to use the reactive Spring WebFlux stack.

## Key changes

* `starter-core` now depends on `spring-boot-starter-webflux` and `reactor-netty` instead of the blocking Web MVC starter.
* Service modules such as `setup-service` include R2DBC and WebFlux dependencies, replacing JPA and servlet-based components.
* Configuration switched to reactive database access via R2DBC while keeping JDBC only for Flyway migrations.
* OpenAPI dependency moved to the WebFlux variant.
* Reactor Kafka is available for non-blocking Kafka consumption.

Refer to module documentation for further migration notes.
