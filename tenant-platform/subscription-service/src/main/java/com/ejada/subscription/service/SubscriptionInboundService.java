package com.ejada.subscription.service;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.dto.ReceiveSubscriptionUpdateRq;

import java.util.UUID;

public interface SubscriptionInboundService {

    ServiceResult<ReceiveSubscriptionNotificationRs> receiveSubscriptionNotification(
            UUID rqUid,
            String token,
            ReceiveSubscriptionNotificationRq request);

    ServiceResult<Void> receiveSubscriptionUpdate(
            UUID rqUid,
            String token,
            ReceiveSubscriptionUpdateRq request);
}
