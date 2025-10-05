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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationReplayService {

    private final InboundNotificationAuditRepository auditRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionEnvironmentIdentifierRepository envIdentifierRepository;
    private final SubscriptionEnvironmentIdentifierMapper envIdentifierMapper;

    private final Map<UUID, ServiceResult<ReceiveSubscriptionNotificationRs>> processedCache =
            new ConcurrentHashMap<>();

    public ServiceResult<ReceiveSubscriptionNotificationRs> replayNotificationIfProcessed(
            final UUID rqUid, final ReceiveSubscriptionNotificationRq rq) {
        if (rqUid == null || rq == null || rq.subscriptionInfo() == null) {
            return null;
        }

        return processedCache.computeIfAbsent(
                rqUid,
                key ->
                        auditRepository
                                .findByRqUidAndEndpoint(key, MarketplaceCallbackEndpoints.NOTIFICATION)
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
                                })
                                .orElse(null));
    }
}
