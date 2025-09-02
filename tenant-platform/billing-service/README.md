# Billing Service

## Scope & Value
- Canonical usage tracking for every feature.
- Overage recording when limits are exceeded and allowed.
- Emits feeds for the external billing system (events and daily summaries).

## Data It Owns
- `feature_usage_event` (immutable ledger, idempotent).
- `feature_usage_counter_current` (current-period totals).
- `tenant_overage` (idempotent, quantity and price metadata) — row-level security.

## Main APIs
- Record usage (append event, update current-period counter).
- Get current usage for `{tenant, feature, period}`.
- Record overage when policy allows.
- Export daily/period summaries (CSV/JSON) for external billing.

## Events
- **Publishes:** UsageRecorded, OverageRecorded, DailyUsageSummaryReady.
- **Consumes:** SubscriptionChanged (align periods), PolicyChanged (optional, price context).

## Folder Conventions
- `adapter` – outbound integrations (e.g., JPA implementations).
- `controller` – REST controllers and API contracts.
- `repository` – Spring Data repositories.
- `dto`, `entity`, `service`, `enums` – domain models and logic.
- Tests mirror the same package structure under `src/test/java` and use `src/test/resources` for configuration.
