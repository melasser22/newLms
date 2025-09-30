package com.ejada.subscription.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.acl.MarketplaceCallbackOrchestrator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionInboundServiceImplTest {

    @Mock private MarketplaceCallbackOrchestrator orchestrator;
    @Mock private ReceiveSubscriptionNotificationRq notificationRq;
    @Mock private ReceiveSubscriptionNotificationRs notificationRs;
    @Mock private ReceiveSubscriptionUpdateRq updateRq;

    @InjectMocks private SubscriptionInboundServiceImpl service;

    @Test
    void delegatesNotificationProcessingToOrchestrator() {
        UUID rqUid = UUID.randomUUID();
        ServiceResult<ReceiveSubscriptionNotificationRs> expected = ServiceResult.ok(notificationRs);
        when(orchestrator.processNotification(rqUid, "token", notificationRq)).thenReturn(expected);

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                service.receiveSubscriptionNotification(rqUid, "token", notificationRq);

        assertThat(result).isSameAs(expected);
        verify(orchestrator).processNotification(rqUid, "token", notificationRq);
    }

    @Test
    void delegatesUpdateProcessingToOrchestrator() {
        UUID rqUid = UUID.randomUUID();
        ServiceResult<Void> expected = ServiceResult.ok(null);
        when(orchestrator.processUpdate(rqUid, "token", updateRq)).thenReturn(expected);

        ServiceResult<Void> result = service.receiveSubscriptionUpdate(rqUid, "token", updateRq);

        assertThat(result).isSameAs(expected);
        verify(orchestrator).processUpdate(rqUid, "token", updateRq);
    }
}
