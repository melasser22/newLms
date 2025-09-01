package com.lms.tenant.service;

import com.lms.tenant.entity.Feature;
import com.lms.tenant.entity.Tenant;
import com.lms.tenant.entity.TenantOverage;
import com.lms.tenant.repository.FeatureRepository;
import com.lms.tenant.repository.TenantOverageRepository;
import com.lms.tenant.repository.TenantRepository;
import com.lms.tenant.service.dto.OverageRecordDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class OverageService {

    private final TenantRepository tenantRepository;
    private final FeatureRepository featureRepository;
    private final TenantOverageRepository overageRepository;

    public OverageService(TenantRepository tenantRepository, FeatureRepository featureRepository,
                          TenantOverageRepository overageRepository) {
        this.tenantRepository = tenantRepository;
        this.featureRepository = featureRepository;
        this.overageRepository = overageRepository;
    }

    @Transactional
    public OverageRecordDto record(UUID tenantId, String featureKey, long quantity,
                                   long unitPriceMinor, String currency,
                                   Instant periodStart, Instant periodEnd, String idemKey) {
        Optional<TenantOverage> existing = overageRepository.findByIdemKey(idemKey);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Feature feature = featureRepository.findById(featureKey).orElseThrow();
        TenantOverage overage = new TenantOverage();
        overage.setTenant(tenant);
        overage.setFeature(feature);
        overage.setQuantity(quantity);
        overage.setUnitPriceMinor(unitPriceMinor);
        overage.setCurrency(currency);
        overage.setPeriodStart(periodStart);
        overage.setPeriodEnd(periodEnd);
        overage.setIdemKey(idemKey);
        TenantOverage saved = overageRepository.save(overage);
        return toDto(saved);
    }

    private OverageRecordDto toDto(TenantOverage entity) {
        return new OverageRecordDto(entity.getId(), entity.getTenant().getId(),
                entity.getFeature().getKey(), entity.getQuantity(),
                entity.getUnitPriceMinor(), entity.getCurrency(),
                entity.getPeriodStart(), entity.getPeriodEnd());
    }
}
