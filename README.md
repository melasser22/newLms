# Learning Management System (LMS)

This repository houses several components:

- **shared-lib** – a collection of reusable Spring Boot starter modules and utilities.
- **lms-setup** – a Spring Boot microservice for reference lookups and tenant/platform configuration.
- **tenant-persistence** – central JPA entities, repositories, and Flyway migrations for the tenant domain.

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

### Build and run the setup service

```bash
cd ../lms-setup
mvn spring-boot:run
```

Set the following variables before running:
=======
The service uses environment variables for database and security settings. Defaults are provided for local development:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CRYPTO_ACTIVE_KID`
- `CRYPTO_LOCAL_DEV_KEY`

### Running Tests

Run unit tests for both modules:

```bash
cd shared-lib && mvn test
cd ../lms-setup && mvn test
```

The build pulls dependencies from Maven Central; ensure network access is available.

### Docker Compose environments

Containerised tooling and application services are now split across two compose files:

- `docker-compose.tools.yml` — infrastructure dependencies such as PostgreSQL, Redis, Kafka, and the OpenTelemetry collector.
- `docker-compose.apps.yml` — LMS application microservices.

Start the full stack by composing both files together:

```bash
docker compose -f docker-compose.tools.yml -f docker-compose.apps.yml up --build
```

You can also start just the shared tooling dependencies when needed:

```bash
docker compose -f docker-compose.tools.yml up -d
```

## Contributing
Contributions are welcome. Please fork the repository and submit pull requests. Ensure tests pass before submitting.

## License
This project is provided under the MIT License.

