# Subscription Service

## Scope & Value
- Source of truth for each tenant's active subscription and period window (trial/active/past_due/canceled).

## Data It Owns
- `tenant_subscription` (and optional `subscription_item`) with row-level security.

## Main APIs
- Start trial, activate, cancel or schedule cancel.
- Get active subscription (tier and period start/end).

## Events
- **Publishes:** SubscriptionStarted, SubscriptionChanged, SubscriptionCanceled.
- **Consumes:** none.
