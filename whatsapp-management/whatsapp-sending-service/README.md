# whatsapp-sending-service

Accepts templated and session messages, enforces WhatsApp's 24-hour window rules, and performs
Redis-backed idempotency and rate limiting. Send requests are queued to Kafka for resilient delivery
workers that call the WhatsApp Business Cloud API, with retries and dead-letter handling for
failures.
