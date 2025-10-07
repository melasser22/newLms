# Kafka Troubleshooting – Controller fails to send `UpdateMetadataRequest`

The platform's local Kafka broker can refuse metadata updates when the
controller connects over an unexpected listener. In that case the broker
accepts a socket connection, immediately closes it, and the controller logs
messages similar to:

```
[RequestSendThread controllerId=0] Controller 0 epoch 1 fails to send request (type: UpdateMetadataRequest ... type=UNKNOWN ... ) to broker localhost:53413 (id: 0 rack: null). Reconnecting to broker.
```

This loop normally happens when the broker advertises an address that does
not match how the controller (or other services) can reach it. The tooling
Docker Compose file publishes two listeners: one that other containers use
and one that the host machine uses for local testing. 【F:docker/tools/docker-compose.yml†L53-L75】

## Fix

1. **Keep the container listener untouched.**
   The `PLAINTEXT://kafka:9092` advertised listener is how every service in
   the compose network (including the controller) reaches the broker.
   Changing it to `localhost` (or another host name that only resolves on the
   host OS) will cause the controller handshake to fail. 【F:docker/tools/docker-compose.yml†L61-L75】

2. **Only customise the host-mapped listener when necessary.**
   If you need to change the port that developers use from the host machine,
   update the `PLAINTEXT_HOST://localhost:29092` entry while keeping the
   `localhost` host name. This listener is never used by other containers, so
   altering it will not break the controller connection. 【F:docker/tools/docker-compose.yml†L61-L75】

3. **Reset local Kafka data after configuration changes.**
   Once the advertised listeners have been fixed, wipe the existing broker
   data to discard corrupted metadata that might still reference the old
   endpoints:

   ```bash
   docker compose -f docker/tools/docker-compose.yml down
   docker volume rm newLms_kafkadata
   docker compose -f docker/tools/docker-compose.yml up --build kafka
   ```

   The `kafkadata` named volume stores metadata and log segments. Removing it
   ensures the broker starts cleanly with the corrected listener configuration.
   Adjust the volume name if your Docker Compose project name differs. 【F:docker/tools/docker-compose.yml†L82-L85】

Following these steps keeps the controller and broker in sync and prevents the
rapid connect/disconnect cycle triggered by `UpdateMetadataRequest` failures.
