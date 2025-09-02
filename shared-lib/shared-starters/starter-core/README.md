# starter-core

Fundamental web infrastructure for Ejada services.

## Use cases
- Global exception handling and standardized API responses.
- JSON configuration and performance optimizations.
- Correlation ID and tenant context filters.
- Utility beans such as `SpringContextHolder` and `DefaultTenantResolver`.

## Usage
Add the dependency:

```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-core</artifactId>
</dependency>
```

Example: annotate controllers with `@RequireTenant` to enforce tenant headers.
