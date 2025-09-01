# Tenant Service

## Scope & Value
- Tenant onboarding and lifecycle: profile, branding, domains, admin settings.
- Org-level overage toggle.

## Data It Owns
- `tenant` (name, slug, timezone/locale, domains[], overage_enabled, status)
- Optional domain verification records and API keys.

## Main APIs
- Tenant CRUD (create/update/suspend/reactivate/archive).
- Domains: add/verify/remove.
- Settings: get/set overage enabled.
- Read-only summary: plan name, period dates (from subscription-service).

## Events
- **Publishes:** TenantCreated, TenantUpdated, OverageToggleChanged.
- **Consumes:** none.
