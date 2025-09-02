# Catalog Service

## Scope & Value
- Plans/tiers, features, base limits, and per-tenant overrides (allow-overage, price override).

## Data It Owns
- `feature`, `product_tier`, `tier_feature_limit`.
- `tenant_feature_override` (row-level security).

## Main APIs
- Manage tiers & features (admin).
- Upsert tenant overrides (enable/limit/allow-overage/price).
- Effective feature: resolve merged result (tier + override) for `{tenant, feature}`.

## Events
- **Publishes:** PolicyChanged (base or override changes).
- **Consumes:** TenantCreated (optional: seed defaults).

## Folder Conventions
- `adapter` – inbound/outbound adapters.
- `controller` – REST API controllers.
- `repository` – Spring Data repositories.
- `entity`, `service` – core domain logic.
- Tests mirror this structure under `src/test/java` and store config in `src/test/resources`.
