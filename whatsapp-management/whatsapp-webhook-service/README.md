# whatsapp-webhook-service

Receives Meta webhook callbacks for delivery receipts, message statuses, and inbound replies. The
service verifies HMAC signatures and source IPs, normalizes and deduplicates events, updates message
logs, and emits Kafka notifications for analytics and conversation workflows.
