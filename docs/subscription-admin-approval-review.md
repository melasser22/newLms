# Subscription Admin Approval Endpoint Review

## Requested capability

The platform requirements describe an administrator-triggered approval flow exposed via
`POST /api/v1/admin/approvals/{approvalRequestId}/approve`. The endpoint should:

1. Validate the administrator's JWT and roles.
2. Load and transition the `SubscriptionApprovalRequest` entity to **APPROVED**.
3. Move the associated `Subscription` to an active/approved state.
4. Persist audit activity, environment identifiers and outbox events.
5. Call downstream services (tenant provisioning, billing, notifications, onboarding, etc.).

## Repository findings

* The subscription service only exposes `/subscription/receiveSubscriptionNotification`
  and `/subscription/receiveSubscriptionUpdate` controller methods. No controller handles
  `/api/v1/admin/approvals/**` routes or any manual approval payloads.
* `ApprovalWorkflowService` and related repositories only support automatic approval flows
  triggered when marketplace callbacks arrive. They do not persist administrator metadata
  or invoke downstream integrations described in the requested sequence.
* Kafka consumers publish provisioning messages **after** an approval message has already
  been produced. There is no producer that emits the `SubscriptionApprovalMessage.APPROVED`
  event from an administrator action.
* No gateway configuration, DTOs, request validators or tests reference the
  `approveSubscription` use case.

## Conclusion

The manual administrator approval workflow is **not implemented** in the current codebase.
Implementing it would require creating a new REST controller and service layer capable of
executing the end-to-end provisioning steps, coordinating with tenant, billing, policy and
notification services, and emitting the relevant activity logs and outbox events.
