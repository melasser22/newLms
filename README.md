# Learning Management System (LMS)

This repository houses several components:

- **shared-lib** – a collection of reusable Spring Boot starter modules and utilities.
- **lms-setup** – a Spring Boot microservice for reference lookups and tenant/platform configuration.
- **tenant-persistence** – central JPA entities, repositories, and Flyway migrations for the tenant domain.
- **api-gateway** – Spring Cloud Gateway instance that fronts the services while reusing shared security and context modules.

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+

### Build shared libraries
Install the shared library modules into the local Maven repository:

```bash
cd shared-lib
mvn clean install
```

### Run the platform with Docker Compose

From the repository root run:

```bash
docker-compose up --build
```

The compose file provisions infrastructure dependencies (PostgreSQL, Redis, Kafka, OTEL collector) alongside the domain services and the API Gateway. Only the gateway is exposed on the host via `http://localhost:8088`; all downstream services remain on the internal Docker network to prevent direct access.

### Running Tests

Run unit tests for the shared libraries:

```bash
cd shared-lib && mvn test
```

The build pulls dependencies from Maven Central; ensure network access is available.

## Contributing
Contributions are welcome. Please fork the repository and submit pull requests. Ensure tests pass before submitting.

## License
This project is provided under the MIT License.

