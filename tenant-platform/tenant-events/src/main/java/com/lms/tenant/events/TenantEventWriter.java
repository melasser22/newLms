package com.lms.tenant.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Writes tenant events to the outbox table using JDBC. */
public class TenantEventWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public TenantEventWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void write(UUID tenantId, String type, Object payload) {
        write(tenantId, type, payload, Instant.now());
    }

    public void write(UUID tenantId, String type, Object payload, Instant availableAt) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize payload", e);
        }
        jdbc.update(
            "INSERT INTO tenant_outbox (id, tenant_id, type, payload, created_at, available_at, attempts, status) " +
            "VALUES (?, ?, ?, cast(? as jsonb), now(), ?, 0, 'NEW')",
            UUID.randomUUID(), tenantId, type, json, Timestamp.from(availableAt));
    }
}
