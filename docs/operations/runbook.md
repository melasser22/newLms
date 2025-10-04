# API Gateway Operational Runbook

This runbook documents day-two operational procedures for the LMS API Gateway platform. It is intended for the SRE and on-call engineering teams that maintain the production and staging environments.

## 1. Service Overview

- **Primary function**: Spring Cloud Gateway that fronts all tenant-facing microservices.
- **Critical dependencies**: Redis Sentinel cluster, Kubernetes ingress controller, shared observability stack (Prometheus, Grafana, Loki), and control plane APIs.
- **Deployments**: Multi-region (primary + backups) with automated canary rollout in staging prior to promotion.

## 2. Monitoring & Dashboards

| Area | Dashboard | Notes |
| --- | --- | --- |
| Gateway health | [Grafana - Gateway SLO](https://grafana.lms.example.com/d/gateway-slo) | Error budget burn, request latency, saturation |
| Redis Sentinel | [Grafana - Redis HA](https://grafana.lms.example.com/d/redis-ha) | Master elections, replica lag |
| Canary rollout | [Argo Rollouts Canary](https://argo.lms.example.com/rollouts) | Staging rollout progress |
| DR posture | [Ops Genie - DR Status](https://ops.lms.example.com/dr) | Region availability, failover state |

Alerts for the above dashboards integrate with PagerDuty (primary) and Microsoft Teams (secondary).

## 3. Common Failure Scenarios

### 3.1 Elevated 5xx Responses

1. Validate whether the failure is local or downstream via the **Gateway Saturation** Grafana panel.
2. If downstream, notify the owning service. If local, check pod logs (`kubectl logs deploy/api-gateway-api-gateway`).
3. Trigger the **circuit breaker manual override** via the actuator endpoint if necessary:
   ```bash
   curl -XPOST -u $USER:$PASSWORD https://gateway.lms.example.com/actuator/circuitbreakers/setup-service/forceOpen
   ```
4. Record the incident in the on-call journal and monitor for recovery.

### 3.2 Redis Sentinel Failover Storm

1. Inspect Sentinel events: `kubectl logs statefulset/redis-sentinel -c sentinel`.
2. Verify pod availability (`kubectl get pods -l app.kubernetes.io/name=redis-sentinel`).
3. If master instability persists, trigger a manual region failover using the DR endpoint (see Section 4.2).
4. After stability returns, re-enable the primary region via the same endpoint.

### 3.3 Canary Deployment Degradation

1. Compare canary metrics against baseline using Grafana's canary dashboard.
2. If error budget burn > 5% in 15 minutes, roll back by setting `canary.weight=0` annotation and reapplying the base deployment manifest.
3. Notify release manager and log the incident with links to dashboards.

## 4. Emergency Procedures

### 4.1 Circuit Breaker Manual Override

- Endpoint: `POST /actuator/circuitbreakers/{name}/forceOpen`
- Credentials: Stored in 1Password under `LMS-Gateway-Actuator`.
- After forcing open, monitor metrics until downstream stabilizes, then close using `forceClose`.

### 4.2 Disaster Recovery Failover Control

- Endpoint: `POST /internal/dr/failover`
- Request payload:
  ```json
  {"region": "eu-west1", "reason": "redis-sentinel-instability"}
  ```
- To restore primary region:
  ```json
  {"region": "primary", "reason": "stability-restored"}
  ```
- The endpoint is protected by mTLS; client certificates are distributed via the secure vault.

## 5. Scaling Guidelines

- **Horizontal scaling**: HPA maintains CPU < 60% and memory < 70%. For sustained peaks, increase `hpa.maxReplicas` via Kustomize overlay and redeploy.
- **Vertical scaling**: Adjust container requests/limits in Helm values. Document any change in this runbook and update the capacity plan spreadsheet.
- **Traffic bursts**: Enable canary promotion to primary only after 30 minutes of stable metrics.

## 6. Maintenance Windows

- Weekly Redis snapshot validation every Sunday 02:00 UTC.
- Monthly DR failover exercise on the first Tuesday.
- Patch Tuesday (second Tuesday) reserved for Kubernetes node reboots.

## 7. On-call Escalation

1. **Primary On-call (Weekdays)**: `@lms-gateway-primary`
2. **Secondary On-call**: `@lms-gateway-secondary`
3. **Escalation Manager**: `operations.manager@lms.example.com`
4. If unresolved after 30 minutes, page the **Platform Director** via PagerDuty escalation policy `LMS-PLATFORM-P1`.

## 8. References

- [Deployment Pipelines](../../.github/workflows/ci-cd.yml)
- [Kustomize manifests](../../deploy/kustomize)
- [Helm charts](../../deploy/helm)

Keep this runbook updated after each incident or significant operational change.
