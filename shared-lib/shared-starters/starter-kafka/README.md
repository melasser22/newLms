# starter-kafka

Convenience auto-configuration for Kafka producers and consumers.

## Use cases
- Opinionated producer/consumer configs with JSON serde.
- Topic naming helpers and envelope wrapper.
- Idempotent listener with pluggable store.
- Observability integration.

## Usage
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-kafka</artifactId>
</dependency>
```

Example properties:

```yaml
shared:
  kafka:
    bootstrap-servers: localhost:9092
```
