# Learning Management System (LMS)

This repository houses several components:

- **shared-lib** – a collection of reusable Spring Boot starter modules and utilities.
- **setup-service** – a Spring Boot microservice for reference lookups and tenant/platform configuration.
- **tenant-platform** – a platform containing shared tenant services and components.

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
cd ../setup-service
mvn spring-boot:run
```

Before running, configure the following environment variables (defaults are provided for local development):
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
cd ../setup-service && mvn test
```

The build pulls dependencies from Maven Central; ensure network access is available.

## Contributing
Contributions are welcome. Please fork the repository and submit pull requests. Ensure tests pass before submitting.

## License
This project is provided under the MIT License.

