# Admin API Gateway

## Scope & Value
- Thin façade for the Admin UI aggregating read models (plan, limits, usage, overage-to-date).
- Writes are delegated to the owning service.

## Data It Owns
- None; maintains only read models or caches.

## Main APIs
- Tenant overview: plan, period dates, usage bars, alerts.
- Bulk exports and search across tenants for internal tools.

## Events
- **Consumes:** All domain events to refresh read models.
