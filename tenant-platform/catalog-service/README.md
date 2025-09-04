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

## Getting Started
To run the service locally:

1. **Build shared libraries**
   ```bash
   cd ../../shared-lib
   mvn clean install
   ```
2. **Start the catalog service**
   ```bash
   cd ../tenant-platform/catalog-service
   mvn spring-boot:run
   ```

The service expects PostgreSQL connection details via standard Spring `spring.datasource.*` properties. Defaults are suitable for local development.

## Usage
- `GET /catalog/effective?tierId={tier}&tenantId={tenant}&featureKey={key}` – resolve the effective feature limits for a tenant.
- `PUT /tenants/{tenantId}/features/{featureKey}/override` – upsert tenant-specific overrides.

OpenAPI documentation is available at `/swagger-ui` when the service is running.

## Use Cases
This service can back any SaaS product that needs to manage feature availability and per-tenant limits. Typical scenarios include:
- Defining product tiers and the features they unlock.
- Allowing sales or support teams to grant temporary upgrades or overage allowances.
- Centralizing feature flags and limit checks for other microservices.
