# email-usage-service

Aggregates delivery analytics per tenant directly from the relational email event log and exposes a
simple reporting API.

## Features
- Aggregate email send, delivery, bounce, open, click, deferral, block, and spam complaint events per tenant.
- Track monthly quota consumption and daily burst limits to enforce tenant-level sending guardrails.
- Generate summary and trend reports (delivery rate, bounce rate, open rate, complaints) for tenant self-service and admin review.
- Exportable daily report output that downstream billing or finance systems can consume.
- Lightweight anomaly detection to flag sudden spikes in bounce rates and alert administrators.
- Persist summarized daily usage to keep reporting fast while still retaining access to raw events when needed.

## HTTP APIs
- `GET /api/tenants/{tenantId}/usage/summary?from=yyyy-MM-dd&to=yyyy-MM-dd`
- `GET /api/tenants/{tenantId}/usage/trends?from=yyyy-MM-dd&to=yyyy-MM-dd`
- `GET /api/tenants/{tenantId}/usage/quota`
- `GET /api/tenants/{tenantId}/usage/anomalies`
- `GET /api/admin/usage/reports/daily?date=yyyy-MM-dd`

Date parameters are optional on summary and trends, defaulting to the last 30 days.
