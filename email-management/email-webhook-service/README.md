# email-webhook-service

Dedicated SendGrid webhook ingestion surface that verifies signatures, deduplicates payloads with
Redis, and emits normalized events to Kafka for downstream consumers.
