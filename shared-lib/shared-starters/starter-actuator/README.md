# starter-actuator

Spring Boot Actuator starter with:
- default exposures (health/info/metrics/prometheus/loggers/threaddump)
- K8s probes enabled
- Common Micrometer tags
- Http exchanges repository (optional)
- Actuator-only security chain (optional)
- Custom endpoint: /actuator/whoami
- SLA report at `/sla/report` backed by a dedicated health indicator

## Usage
Add the dependency:

```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-actuator</artifactId>
</dependency>
```

### SLA report endpoint

The starter registers a lightweight REST controller at `/sla/report`. The
payload mirrors the format of the actuator health endpoint and focuses on the
`slaHealthIndicator` component:

```json
{
  "status": "UP",
  "components": {
    "slaHealthIndicator": {
      "status": "UP",
      "details": {
        "sla_compliant": true,
        "availability_percent": 99.95,
        "last_check": "2025-09-24T14:00:00Z"
      }
    }
  }
}
```

You can customise the indicator details per service:

```yaml
shared:
  actuator:
    sla-report:
      availability-percent: 99.95
      sla-compliant: true
```

Set `shared.actuator.sla-report.enabled=false` if you need to disable either
the indicator or the `/sla/report` endpoint for a particular service.
