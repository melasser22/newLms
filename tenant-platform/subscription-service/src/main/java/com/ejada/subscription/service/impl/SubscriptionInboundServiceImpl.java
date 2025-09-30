package com.ejada.subscription.service.impl;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.acl.marketplace.MarketplaceCallbackFacade;
import com.ejada.subscription.service.SubscriptionInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Bridges the subscription domain service with the marketplace anti-corruption layer.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionInboundServiceImpl implements SubscriptionInboundService {

    private final MarketplaceCallbackFacade marketplaceFacade;

    @Override
    public ServiceResult<ReceiveSubscriptionNotificationRs> receiveSubscriptionNotification(
            final UUID rqUid,
            final String token,
            final ReceiveSubscriptionNotificationRq rq) {
        return marketplaceFacade.handleNotification(rqUid, token, rq);
    }

    @Override
    public ServiceResult<Void> receiveSubscriptionUpdate(
            final UUID rqUid,
            final String token,
            final ReceiveSubscriptionUpdateRq rq) {
        return marketplaceFacade.handleUpdate(rqUid, token, rq);
    }
}
