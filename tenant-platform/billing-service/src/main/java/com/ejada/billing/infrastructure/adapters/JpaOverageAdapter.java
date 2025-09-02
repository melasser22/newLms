package com.ejada.billing.infrastructure.adapters;

import com.ejada.billing.domain.dtos.OverageResponse;
import com.ejada.billing.domain.dtos.RecordOverageRequest;
import com.ejada.billing.domain.entities.TenantOverage;
import com.ejada.billing.domain.enums.OverageStatus;
import com.ejada.billing.infrastructure.repositories.TenantOverageRepository;
import com.ejada.billing.application.ports.OveragePort;
import com.ejada.common.exception.JsonSerializationException;
import com.ejada.common.json.JsonUtils;
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

    public JpaOverageAdapter(TenantOverageRepository repo) {
        this.repo = repo;
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
        o.setPeriodStart(req.periodStart() == null ? Instant.now() : req.periodStart());
        o.setPeriodEnd(req.periodEnd() == null ? Instant.now() : req.periodEnd());
        o.setStatus(OverageStatus.RECORDED);
        o.setIdempotencyKey(req.idempotencyKey());
        try {
            o.setMetadataJson(JsonUtils.toJson(req.metadata() == null ? Map.of() : req.metadata()));
        } catch (JsonSerializationException e) {
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

