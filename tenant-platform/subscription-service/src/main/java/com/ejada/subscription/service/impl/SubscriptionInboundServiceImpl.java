package com.ejada.subscription.service.impl;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.acl.MarketplaceCallbackOrchestrator;
import com.ejada.subscription.service.SubscriptionInboundService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionInboundServiceImpl implements SubscriptionInboundService {

    private final MarketplaceCallbackOrchestrator orchestrator;

    @Override
    public ServiceResult<ReceiveSubscriptionNotificationRs> receiveSubscriptionNotification(
            final UUID rqUid, final String token, final ReceiveSubscriptionNotificationRq request) {
        return orchestrator.processNotification(rqUid, token, request);
    }

    @Override
    public ServiceResult<Void> receiveSubscriptionUpdate(
            final UUID rqUid, final String token, final ReceiveSubscriptionUpdateRq request) {
        return orchestrator.processUpdate(rqUid, token, request);
    }
}
