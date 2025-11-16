# email-sending-service

Exposes the sending APIs, performs Redis-backed idempotency and rate-limiting, and publishes
normalized envelopes onto Kafka topics for async delivery workers.
