# whatsapp-template-service

Provides CRUD, validation, and Meta submission workflows for WhatsApp Highly Structured Message
templates. The service manages attachment uploads, caches Meta media IDs for reuse, tracks approval
statuses and versions, and publishes Kafka events when template changes need to be synchronized
with sending or usage pipelines.
