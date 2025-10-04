package com.ejada.subscription.service.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.marketplace.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.dto.admin.AdminApproveSubscriptionRequest;
import com.ejada.subscription.dto.admin.AdminApproveSubscriptionResponse;
import com.ejada.subscription.kafka.SubscriptionApprovalPublisher;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionActivityLog;
import com.ejada.subscription.model.SubscriptionApprovalRequest;
import com.ejada.subscription.model.SubscriptionApprovalStatus;
import com.ejada.subscription.repository.SubscriptionActivityLogRepository;
import com.ejada.subscription.repository.SubscriptionApprovalRequestRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminApprovalServiceTest {

    @Mock private SubscriptionApprovalRequestRepository approvalRequestRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionActivityLogRepository activityLogRepository;
    @Mock private SubscriptionApprovalPublisher approvalPublisher;
    @Mock private ApprovalActorProvider actorProvider;

    private AdminApprovalService service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        service = new AdminApprovalService(
                approvalRequestRepository,
                subscriptionRepository,
                activityLogRepository,
                approvalPublisher,
                actorProvider,
                objectMapper);
    }

    @Test
    void approveTransitionsEntitiesAndPublishesEvent() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(10L);
        subscription.setExtSubscriptionId(123L);
        subscription.setExtCustomerId(456L);
        subscription.setApprovalStatus(SubscriptionApprovalStatus.PENDING_APPROVAL.name());
        subscription.setSubscriptionSttsCd("PENDING_APPROVAL");

        CustomerInfoDto customerInfo = new CustomerInfoDto(
                "Customer EN",
                "Customer AR",
                "TYPE",
                "1234",
                "SA",
                "RUH",
                "Address",
                "Address",
                "ops@example.com",
                "+971500000000");

        SubscriptionApprovalRequest approvalRequest = new SubscriptionApprovalRequest();
        approvalRequest.setApprovalRequestId(5L);
        approvalRequest.setStatus("PENDING");
        approvalRequest.setSubscription(subscription);
        approvalRequest.setTenantInfoJson(objectMapper.writeValueAsString(customerInfo));

        when(approvalRequestRepository.findById(5L)).thenReturn(Optional.of(approvalRequest));
        when(actorProvider.currentActor())
                .thenReturn(new ApprovalActorProvider.ApprovalActor("approver", "Approver", "approver@example.com"));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalRequestRepository.save(any(SubscriptionApprovalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                UUID.randomUUID(),
                subscription.getExtSubscriptionId(),
                subscription.getExtCustomerId(),
                customerInfo.customerNameEn(),
                customerInfo.customerNameAr(),
                customerInfo.email(),
                customerInfo.mobileNo(),
                "TEN-123",
                "Tenant",
                customerInfo.email(),
                customerInfo.mobileNo(),
                "role",
                OffsetDateTime.now(),
                "notes");
        when(approvalPublisher.publishApprovalDecision(
                        any(), any(), any(Subscription.class), any(), any(), any(), any()))
                .thenReturn(message);

        AdminApproveSubscriptionRequest request =
                new AdminApproveSubscriptionRequest("All good", List.of("PAYMENT_VERIFIED"), true);

        ServiceResult<AdminApproveSubscriptionResponse> result = service.approve(5L, request);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.payload()).isNotNull();
        assertThat(result.payload().approvalRequestId()).isEqualTo(5L);
        assertThat(subscription.getApprovalStatus()).isEqualTo(SubscriptionApprovalStatus.APPROVED.name());
        assertThat(subscription.getSubscriptionSttsCd()).isEqualTo("ACTIVE");
        assertThat(approvalRequest.getStatus()).isEqualTo("APPROVED");
        assertThat(approvalRequest.getApprovedBy()).isEqualTo("approver");

        ArgumentCaptor<SubscriptionActivityLog> logCaptor = ArgumentCaptor.forClass(SubscriptionActivityLog.class);
        verify(activityLogRepository).save(logCaptor.capture());
        SubscriptionActivityLog logEntry = logCaptor.getValue();
        assertThat(logEntry.getSubscription()).isEqualTo(subscription);
        assertThat(logEntry.getActivityType()).isEqualTo("APPROVED");
        assertThat(logEntry.getPerformedBy()).isEqualTo("approver");
        assertThat(logEntry.getMetadata()).contains("PAYMENT_VERIFIED");

        verify(approvalPublisher)
                .publishApprovalDecision(
                        eq(SubscriptionApprovalAction.APPROVED),
                        any(UUID.class),
                        any(Subscription.class),
                        any(CustomerInfoDto.class),
                        any(),
                        any(),
                        any());
    }

    @Test
    void approveReturnsErrorWhenRequestMissing() {
        when(approvalRequestRepository.findById(anyLong())).thenReturn(Optional.empty());

        ServiceResult<AdminApproveSubscriptionResponse> result =
                service.approve(999L, new AdminApproveSubscriptionRequest(null, List.of(), false));

        assertThat(result.success()).isFalse();
        verify(subscriptionRepository, never()).save(any());
        verify(approvalPublisher, never())
                .publishApprovalDecision(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveReturnsErrorWhenAlreadyProcessed() {
        SubscriptionApprovalRequest approvalRequest = new SubscriptionApprovalRequest();
        approvalRequest.setApprovalRequestId(7L);
        approvalRequest.setStatus("APPROVED");
        when(approvalRequestRepository.findById(7L)).thenReturn(Optional.of(approvalRequest));

        ServiceResult<AdminApproveSubscriptionResponse> result =
                service.approve(7L, new AdminApproveSubscriptionRequest(null, List.of(), false));

        assertThat(result.success()).isFalse();
        verify(subscriptionRepository, never()).save(any());
        verify(approvalPublisher, never())
                .publishApprovalDecision(any(), any(), any(), any(), any(), any(), any());
    }
}
