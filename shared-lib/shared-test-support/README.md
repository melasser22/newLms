# shared-test-support

Utilities for integration tests using Testcontainers.

## Features
- `IntegrationTestSupport` base class to spin up Postgres and Redis containers.
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
