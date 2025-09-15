# starter-resilience

Auto-configures Resilience4j components with sensible defaults.

## Configuration Properties
```yaml
shared:
  resilience:
    http-timeout-ms: 5000
    connect-timeout-ms: 2000
```

## Usage
Add the dependency:
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-resilience</artifactId>
</dependency>
```
