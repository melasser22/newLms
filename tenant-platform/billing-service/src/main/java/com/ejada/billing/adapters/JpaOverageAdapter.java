package com.ejada.billing.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.billing.dto.OverageResponse;
import com.ejada.billing.dto.RecordOverageRequest;
import com.ejada.billing.entity.TenantOverage;
import com.ejada.billing.enums.OverageStatus;
import com.ejada.billing.repo.TenantOverageRepository;
import com.ejada.billing.service.OveragePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA implementation of {@link OveragePort}.
 */
@Component
public class JpaOverageAdapter implements OveragePort {

    private final TenantOverageRepository repo;
    private final ObjectMapper objectMapper;

    public JpaOverageAdapter(TenantOverageRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public OverageResponse recordOverage(UUID tenantId, UUID subscriptionId, RecordOverageRequest request) {
        if (request.idempotencyKey() != null) {
            return repo.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey())
                    .map(this::toResponse)
                    .orElseGet(() -> saveNew(tenantId, subscriptionId, request));
        }
        return saveNew(tenantId, subscriptionId, request);
    }

    private OverageResponse saveNew(UUID tenantId, UUID subscriptionId, RecordOverageRequest req) {
        TenantOverage o = new TenantOverage();
        o.setId(UUID.randomUUID());
        o.setTenantId(tenantId);
        o.setSubscriptionId(subscriptionId);
        o.setFeatureKey(req.featureKey());
        o.setQuantity(req.quantity());
        o.setUnitPriceMinor(req.unitPriceMinor() == null ? 0L : req.unitPriceMinor());
        o.setCurrency(req.currency() == null ? "USD" : req.currency());
        o.setOccurredAt(req.occurredAt() == null ? Instant.now() : req.occurredAt());
        o.setPeriodStart(req.periodStart());
        o.setPeriodEnd(req.periodEnd());
        o.setStatus(OverageStatus.RECORDED);
        o.setIdempotencyKey(req.idempotencyKey());
        try {
            o.setMetadataJson(objectMapper.writeValueAsString(req.metadata() == null ? Map.of() : req.metadata()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        TenantOverage saved = repo.save(o);
        return toResponse(saved);
    }

    private OverageResponse toResponse(TenantOverage o) {
        return new OverageResponse(
                o.getId(),
                o.getTenantId(),
                o.getFeatureKey(),
                o.getQuantity(),
                o.getUnitPriceMinor(),
                o.getCurrency(),
                o.getOccurredAt(),
                o.getPeriodStart(),
                o.getPeriodEnd(),
                o.getStatus().name()
        );
    }
}

