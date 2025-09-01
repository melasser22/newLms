# starter-core

Fundamental web infrastructure for LMS services.

## Use cases
- Global exception handling and standardized API responses.
- JSON configuration and performance optimizations.
- Correlation ID and tenant context filters.
- Utility beans such as `SpringContextHolder` and `DefaultTenantResolver`.

## Usage
Add the dependency:

```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-core</artifactId>
</dependency>
```

Example: annotate controllers with `@RequireTenant` to enforce tenant headers.
