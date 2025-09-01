package com.lms.tenant.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.tenant.events.entity.TenantOutboxEvent;
import com.lms.tenant.events.repo.TenantOutboxEventRepository;
import java.time.Instant;
import java.util.UUID;

/** Writes tenant events to the outbox table using JPA. */
public class TenantEventWriter {
    private final TenantOutboxEventRepository repository;
    private final ObjectMapper mapper;

    public TenantEventWriter(TenantOutboxEventRepository repository, ObjectMapper mapper) {
        this.repository = repository;
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
        TenantOutboxEvent event = new TenantOutboxEvent();
        event.setTenantId(tenantId.toString());
        event.setType(type);
        event.setPayload(json);
        event.setAvailableAt(availableAt);
        repository.save(event);
    }
}
