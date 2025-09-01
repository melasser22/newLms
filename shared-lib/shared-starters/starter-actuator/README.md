# starter-actuator

Spring Boot Actuator starter with:
- default exposures (health/info/metrics/prometheus/loggers/threaddump)
- K8s probes enabled
- Common Micrometer tags
- Http exchanges repository (optional)
- Actuator-only security chain (optional)
- Custom endpoint: /actuator/whoami

## Usage
Add the dependency:

```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-actuator</artifactId>
</dependency>
```
