package com.ejada.subscription.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.subscription.dto.AdminUserInfoDto;
import com.ejada.common.marketplace.subscription.dto.CustomerInfoDto;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.marketplace.subscription.dto.SubscriptionInfoDto;
import com.ejada.common.marketplace.subscription.dto.SubscriptionUpdateType;
import com.ejada.subscription.acl.service.IdempotentRequestService;
import com.ejada.subscription.acl.service.NotificationAuditService;
import com.ejada.subscription.acl.service.NotificationReplayService;
import com.ejada.subscription.acl.service.SubscriptionOutboxService;
import com.ejada.subscription.kafka.SubscriptionApprovalPublisher;
import com.ejada.subscription.mapper.SubscriptionAdditionalServiceMapper;
import com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper;
import com.ejada.subscription.mapper.SubscriptionFeatureMapper;
import com.ejada.subscription.mapper.SubscriptionMapper;
import com.ejada.subscription.mapper.SubscriptionProductPropertyMapper;
import com.ejada.subscription.mapper.SubscriptionUpdateEventMapper;
import com.ejada.subscription.model.InboundNotificationAudit;
import com.ejada.subscription.model.Subscription;
import com.ejada.subscription.model.SubscriptionUpdateEvent;
import com.ejada.subscription.model.SubscriptionApprovalRequest;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionProductPropertyRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.ejada.subscription.repository.SubscriptionUpdateEventRepository;
import com.ejada.subscription.service.approval.ApprovalWorkflowService;
import com.ejada.subscription.service.approval.ApprovalWorkflowService.SubmissionResult;
import com.ejada.subscription.tenant.TenantLink;
import com.ejada.subscription.tenant.TenantLinkFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketplaceCallbackOrchestratorTest {

    @Mock private SubscriptionRepository subscriptionRepo;
    @Mock private SubscriptionFeatureRepository featureRepo;
    @Mock private SubscriptionAdditionalServiceRepository additionalServiceRepo;
    @Mock private SubscriptionProductPropertyRepository propertyRepo;
    @Mock private SubscriptionEnvironmentIdentifierRepository envIdRepo;
    @Mock private SubscriptionUpdateEventRepository updateEventRepo;
    @Mock private SubscriptionMapper subscriptionMapper;
    @Mock private SubscriptionFeatureMapper featureMapper;
    @Mock private SubscriptionAdditionalServiceMapper additionalServiceMapper;
    @Mock private SubscriptionProductPropertyMapper propertyMapper;
    @Mock private SubscriptionEnvironmentIdentifierMapper envIdMapper;
    @Mock private SubscriptionUpdateEventMapper updateEventMapper;
    @Mock private SubscriptionApprovalPublisher approvalPublisher;
    @Mock private ApprovalWorkflowService approvalWorkflowService;
    @Mock private NotificationReplayService notificationReplayService;
    @Mock private NotificationAuditService notificationAuditService;
    @Mock private SubscriptionOutboxService subscriptionOutboxService;
    @Mock private IdempotentRequestService idempotentRequestService;

    private MarketplaceCallbackOrchestrator orchestrator;
    private TenantLinkFactory tenantLinkFactory;

    @BeforeEach
    void setUp() {
        tenantLinkFactory = new TenantLinkFactory();
        orchestrator = new MarketplaceCallbackOrchestrator(
                subscriptionRepo,
                featureRepo,
                additionalServiceRepo,
                propertyRepo,
                envIdRepo,
                updateEventRepo,
                subscriptionMapper,
                featureMapper,
                additionalServiceMapper,
                propertyMapper,
                envIdMapper,
                updateEventMapper,
                approvalPublisher,
                tenantLinkFactory,
                approvalWorkflowService,
                notificationReplayService,
                notificationAuditService,
                subscriptionOutboxService,
                idempotentRequestService);
    }

    @Test
    void processNotificationPublishesApprovalRequestForNewSubscription() {
        UUID rqUid = UUID.randomUUID();
        SubscriptionInfoDto subscriptionInfo = new SubscriptionInfoDto(
                123L,
                456L,
                789L,
                1L,
                "Tier",
                "طبقة",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "ACTIVE",
                "PORTAL",
                Boolean.TRUE,
                5L,
                "FULL",
                Boolean.TRUE,
                10L,
                "PERIOD",
                BigDecimal.ONE,
                "MONTH",
                "L",
                Boolean.TRUE,
                null,
                null,
                List.of(),
                List.of());

        ReceiveSubscriptionNotificationRq request = new ReceiveSubscriptionNotificationRq(
                new CustomerInfoDto(
                        "Ejada",
                        "اجادة",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "ops@example.com",
                        "+966500000000"),
                new AdminUserInfoDto("admin", "EN", "1234567890", "admin@example.com"),
                subscriptionInfo,
                List.of());

        InboundNotificationAudit savedAudit = new InboundNotificationAudit();
        savedAudit.setInboundNotificationAuditId(15L);
        when(notificationReplayService.replayNotificationIfProcessed(rqUid, request)).thenReturn(null);
        when(notificationAuditService.recordInboundAudit(rqUid, "token", request, "RECEIVE_NOTIFICATION"))
                .thenReturn(savedAudit);

        when(subscriptionRepo.findByExtSubscriptionIdAndExtCustomerId(123L, 456L)).thenReturn(Optional.empty());

        Subscription mapped = new Subscription();
        mapped.setExtSubscriptionId(123L);
        mapped.setExtCustomerId(456L);
        when(subscriptionMapper.toEntity(subscriptionInfo)).thenReturn(mapped);

        when(subscriptionRepo.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription value = invocation.getArgument(0);
                    if (value.getSubscriptionId() == null) {
                        value.setSubscriptionId(200L);
                    }
                    return value;
                });

        when(featureRepo.findBySubscriptionSubscriptionId(200L)).thenReturn(List.of());
        when(additionalServiceRepo.findBySubscriptionSubscriptionId(200L)).thenReturn(List.of());
        when(propertyRepo.findBySubscriptionSubscriptionId(200L)).thenReturn(List.of());

        SubscriptionApprovalRequest approvalRequest = new SubscriptionApprovalRequest();
        approvalRequest.setApprovalRequestId(88L);
        when(approvalWorkflowService.submitForApproval(mapped, request, true))
                .thenReturn(SubmissionResult.pending(mapped, approvalRequest));

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                orchestrator.processNotification(rqUid, "token", request);

        assertThat(result.success()).isTrue();
        assertThat(result.statusCode()).isEqualTo("I000001");
        verify(notificationReplayService).replayNotificationIfProcessed(rqUid, request);
        verify(notificationAuditService)
                .recordInboundAudit(rqUid, "token", request, "RECEIVE_NOTIFICATION");
        verify(approvalPublisher)
                .publishApprovalRequest(eq(rqUid), eq(request), any(Subscription.class));
    }

    @Test
    void processNotificationAutoApprovesWhenWorkflowApproves() {
        UUID rqUid = UUID.randomUUID();
        SubscriptionInfoDto subscriptionInfo = new SubscriptionInfoDto(
                321L,
                654L,
                999L,
                2L,
                "Enterprise",
                "مشروع",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "ACTIVE",
                "MARKETPLACE",
                Boolean.FALSE,
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                null,
                null,
                List.of(),
                List.of());

        ReceiveSubscriptionNotificationRq request = new ReceiveSubscriptionNotificationRq(
                new CustomerInfoDto("Acme", null, null, null, null, null, null, null, "owner@acme.com", null),
                new AdminUserInfoDto("owner", "EN", "1234567890", "owner@acme.com"),
                subscriptionInfo,
                List.of());

        InboundNotificationAudit savedAudit = new InboundNotificationAudit();
        savedAudit.setInboundNotificationAuditId(42L);
        when(notificationReplayService.replayNotificationIfProcessed(rqUid, request)).thenReturn(null);
        when(notificationAuditService.recordInboundAudit(rqUid, "token", request, "RECEIVE_NOTIFICATION"))
                .thenReturn(savedAudit);

        when(subscriptionRepo.findByExtSubscriptionIdAndExtCustomerId(321L, 654L)).thenReturn(Optional.empty());

        Subscription mapped = new Subscription();
        mapped.setExtSubscriptionId(321L);
        mapped.setExtCustomerId(654L);
        when(subscriptionMapper.toEntity(subscriptionInfo)).thenReturn(mapped);

        when(subscriptionRepo.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription value = invocation.getArgument(0);
                    if (value.getSubscriptionId() == null) {
                        value.setSubscriptionId(300L);
                    }
                    return value;
                });

        when(featureRepo.findBySubscriptionSubscriptionId(300L)).thenReturn(List.of());
        when(additionalServiceRepo.findBySubscriptionSubscriptionId(300L)).thenReturn(List.of());
        when(propertyRepo.findBySubscriptionSubscriptionId(300L)).thenReturn(List.of());
        when(envIdRepo.findBySubscriptionSubscriptionId(300L)).thenReturn(List.of());
        when(envIdMapper.toDtoList(List.of())).thenReturn(List.of());

        SubscriptionApprovalRequest approvalRequest = new SubscriptionApprovalRequest();
        when(approvalWorkflowService.submitForApproval(mapped, request, true))
                .thenReturn(SubmissionResult.autoApproved(mapped, approvalRequest, "LOW_VALUE"));

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                orchestrator.processNotification(rqUid, "token", request);

        TenantLink expectedTenantLink = tenantLinkFactory.resolve(request, mapped);

        assertThat(result.statusCode()).isEqualTo("I000000");
        verify(subscriptionOutboxService)
                .emit(eq("SUBSCRIPTION"), eq("300"), eq("CREATED_OR_UPDATED"), ArgumentMatchers.<Map<String, ?>>any());
        verify(idempotentRequestService).record(rqUid, "RECEIVE_NOTIFICATION", request);
        verify(notificationAuditService)
                .markSuccess(eq(42L), eq("I000000"), eq("Subscription auto-approved"), eq(null));
        verify(approvalPublisher)
                .publishApprovalDecision(
                        eq(SubscriptionApprovalAction.APPROVED),
                        eq(rqUid),
                        eq(mapped),
                        eq(request.customerInfo()),
                        eq(request.adminUserInfo()),
                        eq(expectedTenantLink),
                        eq("LOW_VALUE"));
    }

    @Test
    void processNotificationReturnsCachedResponseWhenAlreadyProcessed() {
        UUID rqUid = UUID.randomUUID();
        SubscriptionInfoDto subscriptionInfo = new SubscriptionInfoDto(
                123L,
                456L,
                789L,
                1L,
                "Tier",
                "Tier",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "ACTIVE",
                "PORTAL",
                Boolean.TRUE,
                5L,
                "FULL",
                Boolean.TRUE,
                10L,
                "PERIOD",
                BigDecimal.ONE,
                "MONTH",
                "L",
                Boolean.TRUE,
                null,
                null,
                List.of(),
                List.of());
        ReceiveSubscriptionNotificationRq request = new ReceiveSubscriptionNotificationRq(
                new CustomerInfoDto(null, null, null, null, null, null, null, null, "ops@example.com", null),
                new AdminUserInfoDto("admin", "EN", "1234567890", "admin@example.com"),
                subscriptionInfo,
                List.of());

        ServiceResult<ReceiveSubscriptionNotificationRs> cached =
                ServiceResult.ok(new ReceiveSubscriptionNotificationRs(Boolean.TRUE, List.of()));
        when(notificationReplayService.replayNotificationIfProcessed(rqUid, request)).thenReturn(cached);

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                orchestrator.processNotification(rqUid, "token", request);

        assertThat(result).isEqualTo(cached);
        verify(notificationReplayService).replayNotificationIfProcessed(rqUid, request);
        verifyNoInteractions(notificationAuditService);
    }

    @Test
    void processUpdateTransitionsStatusAndHandlesOutboxFailure() {
        UUID rqUid = UUID.randomUUID();
        ReceiveSubscriptionUpdateRq request = new ReceiveSubscriptionUpdateRq(
                123L, 456L, SubscriptionUpdateType.TERMINATED);

        when(updateEventRepo.findByRqUid(rqUid)).thenReturn(Optional.empty());

        InboundNotificationAudit savedAudit = new InboundNotificationAudit();
        savedAudit.setInboundNotificationAuditId(10L);
        when(notificationAuditService.recordInboundAudit(rqUid, "token", request, "RECEIVE_UPDATE"))
                .thenReturn(savedAudit);

        SubscriptionUpdateEvent updateEvent = new SubscriptionUpdateEvent();
        when(updateEventMapper.toEvent(eq(request), eq(rqUid))).thenReturn(updateEvent);
        when(updateEventRepo.save(updateEvent)).thenReturn(updateEvent);

        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(200L);
        subscription.setExtSubscriptionId(123L);
        subscription.setExtCustomerId(456L);
        subscription.setSubscriptionSttsCd("ACTIVE");
        when(subscriptionRepo.findByExtSubscriptionId(123L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepo.save(subscription)).thenReturn(subscription);

        doThrow(new RuntimeException("outbox down"))
                .when(subscriptionOutboxService)
                .emit(eq("SUBSCRIPTION"), eq("200"), eq("STATUS_CHANGED"), ArgumentMatchers.<Map<String, ?>>any());

        ServiceResult<Void> result = orchestrator.processUpdate(rqUid, "token", request);

        assertThat(result.statusCode()).isEqualTo("I000000");
        assertThat(subscription.getSubscriptionSttsCd()).isEqualTo("CANCELED");
        assertThat(subscription.getIsDeleted()).isTrue();
        verify(idempotentRequestService).record(rqUid, "RECEIVE_UPDATE", request);
        verify(notificationAuditService)
                .markSuccess(eq(10L), eq("I000000"), eq("Successful Operation"), eq(null));
        verify(subscriptionOutboxService)
                .emit(eq("SUBSCRIPTION"), eq("200"), eq("STATUS_CHANGED"), ArgumentMatchers.<Map<String, ?>>any());
    }

}
