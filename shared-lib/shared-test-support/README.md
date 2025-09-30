# shared-test-support

Utilities for integration tests using Testcontainers.

> **Note**
> The library bundles a [`testcontainers.properties`](src/main/resources/testcontainers.properties)
> file that enables privileged mode for Ryuk. This avoids connection errors on
> Windows environments that expose Docker via named pipes (e.g. when the log
> contains `Found Docker environment with local Npipe socket`).

## Features
- `PostgresTestExtension`, `RedisTestExtension`, and `SetupSchemaExtension` for opt-in container and schema support.
- Helpers for generating JWTs in tests.

## Usage
Add the dependency to test scope:
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>shared-test-support</artifactId>
  <scope>test</scope>
</dependency>
```
