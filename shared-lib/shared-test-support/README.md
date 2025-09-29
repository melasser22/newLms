# shared-test-support

Utilities for integration tests using Testcontainers.

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
