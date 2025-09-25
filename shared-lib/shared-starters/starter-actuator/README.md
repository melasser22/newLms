# starter-actuator

Spring Boot Actuator starter with:
- default exposures (health/info/metrics/prometheus/loggers/threaddump)
- K8s probes enabled
- Common Micrometer tags
- Http exchanges repository (optional)
- Actuator-only security chain (optional)
- Custom endpoint: /actuator/whoami
- SLA report at `/sla/report` with build/runtime metadata

## Usage
Add the dependency:

```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-actuator</artifactId>
</dependency>
```

### SLA report endpoint

The starter registers a lightweight REST controller at `/sla/report`. It provides
service name, build metadata, uptime, host details, and the current health/info
snapshots. You can enrich the response with optional contact data:

```yaml
shared:
  actuator:
    sla-report:
      owner: Platform SRE
      contact: sre@example.com
      description: Security service availability contact
```

Set `shared.actuator.sla-report.enabled=false` if you need to disable the
endpoint for a particular service.
