# starter-observability

Sets up logging and metrics using Micrometer and OpenTelemetry.

## Use cases
- Preconfigured `logback-spring.xml` with trace/span IDs.
- Auto-configures Micrometer exporters.
- Enables default tracing for HTTP clients and servers.

## Usage
```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-observability</artifactId>
</dependency>
```
