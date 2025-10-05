package com.ejada.subscription.acl.service;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.acl.MarketplaceCallbackEndpoints;
import com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper;
import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import com.ejada.subscription.repository.InboundNotificationAuditRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationReplayService {

    private final InboundNotificationAuditRepository auditRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEnvironmentIdentifierRepository envIdentifierRepository;
    private final SubscriptionEnvironmentIdentifierMapper envIdentifierMapper;

    private final Cache<UUID, Optional<ServiceResult<ReceiveSubscriptionNotificationRs>>>
            processedCache =
                    Caffeine.newBuilder()
                            .maximumSize(10_000)
                            .expireAfterWrite(Duration.ofHours(1))
                            .build();

    public ServiceResult<ReceiveSubscriptionNotificationRs> replayNotificationIfProcessed(
            final UUID rqUid, final ReceiveSubscriptionNotificationRq rq) {
        if (rqUid == null || rq == null || rq.subscriptionInfo() == null) {
            return null;
        }

        Optional<ServiceResult<ReceiveSubscriptionNotificationRs>> cachedResult =
                processedCache.getIfPresent(rqUid);
        if (cachedResult != null) {
            return cachedResult.orElse(null);
        }

        Optional<ServiceResult<ReceiveSubscriptionNotificationRs>> computedResult =
                auditRepository
                        .findByRqUidAndEndpoint(rqUid, MarketplaceCallbackEndpoints.NOTIFICATION)
                        .filter(audit -> Boolean.TRUE.equals(audit.getProcessed()))
                        .map(audit -> {
                            var info = rq.subscriptionInfo();
                            var maybeSub =
                                    subscriptionRepository.findByExtSubscriptionIdAndExtCustomerId(
                                            info.subscriptionId(), info.customerId());
                            List<SubscriptionEnvironmentIdentifier> ids = maybeSub
                                    .map(sub ->
                                            envIdentifierRepository.findBySubscriptionSubscriptionId(
                                                    sub.getSubscriptionId()))
                                    .orElseGet(List::of);
                            ReceiveSubscriptionNotificationRs rs =
                                    new ReceiveSubscriptionNotificationRs(
                                            Boolean.TRUE, envIdentifierMapper.toDtoList(ids));
                            return ServiceResult.ok(rs);
                        });

        processedCache.put(rqUid, computedResult);
        return computedResult.orElse(null);
    }
}
