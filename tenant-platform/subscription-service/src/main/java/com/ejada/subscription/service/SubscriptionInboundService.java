package com.ejada.subscription.service;

import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.ServiceResult;

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
