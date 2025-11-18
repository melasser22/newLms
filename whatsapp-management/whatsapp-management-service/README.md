# whatsapp-management-service

API gateway that fronts the WhatsApp tenant experience. It owns onboarding through Meta's embedded
signup, stores WABA identifiers and credentials in GCP Secret Manager, applies per-tenant rate limits
and RBAC, and routes traffic to the downstream WhatsApp template, sending, webhook, and usage
services. The gateway also aggregates responses for the tenant/admin APIs exposed to the LMS portal.
