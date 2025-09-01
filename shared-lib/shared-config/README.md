# shared-config

Auto-configuration that exposes environment and application properties.

## Use cases
- Bind environment details such as region and stage via `EnvironmentProperties`.
- Provide `AppProperties` for application name and version.
- Automatically log active profiles and environment at startup.

## Usage
```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>shared-config</artifactId>
</dependency>
```

Example `application.yml`:

```yaml
shared:
  env: dev
  app:
    name: orders-service
    version: 1.0.0
```
