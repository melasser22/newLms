package com.ejada.tenant.subscription.service;

import com.ejada.tenant.subscription.domain.SubscriptionStatus;
import com.ejada.tenant.subscription.domain.TenantSubscription;
import com.ejada.tenant.subscription.repo.TenantSubscriptionRepository;
import com.shared.subscription.api.SubscriptionDto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService {

    private final TenantSubscriptionRepository repository;

    public SubscriptionService(TenantSubscriptionRepository repository) {
        this.repository = repository;
    }

    public SubscriptionDto startTrial(UUID tenantId) {
        repository.findActiveByTenantId(tenantId)
            .ifPresent(s -> { throw new IllegalStateException("Active subscription exists"); });
        TenantSubscription sub = new TenantSubscription(UUID.randomUUID(), tenantId, SubscriptionStatus.TRIAL, true);
        repository.save(sub);
        return toDto(sub);
    }

    public SubscriptionDto activate(UUID tenantId) {
        TenantSubscription sub = repository.findActiveByTenantId(tenantId)
            .map(existing -> {
                existing.setStatus(SubscriptionStatus.ACTIVE);
                return existing;
            })
            .orElseGet(() -> new TenantSubscription(UUID.randomUUID(), tenantId, SubscriptionStatus.ACTIVE, true));
        repository.save(sub);
        return toDto(sub);
    }

    public void cancel(UUID tenantId, UUID subscriptionId) {
        TenantSubscription sub = repository.findById(subscriptionId)
            .filter(s -> s.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        sub.setActive(false);
        sub.setStatus(SubscriptionStatus.CANCELED);
        repository.save(sub);
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionDto> findActiveSubscription(UUID tenantId) {
        return repository.findActiveByTenantId(tenantId).map(this::toDto);
    }

    SubscriptionDto toDto(TenantSubscription sub) {
        return new SubscriptionDto(sub.getId(), sub.getTenantId(), sub.getStatus().name());
    }
}
