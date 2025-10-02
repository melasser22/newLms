package com.ejada.subscription.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.marketplace.subscription.dto.AdminUserInfoDto;
import com.ejada.common.marketplace.subscription.dto.CustomerInfoDto;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.common.marketplace.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.marketplace.subscription.dto.SubscriptionInfoDto;
import com.ejada.common.marketplace.subscription.dto.SubscriptionUpdateType;
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
import com.ejada.subscription.repository.InboundNotificationAuditRepository;
import com.ejada.subscription.repository.IdempotentRequestRepository;
import com.ejada.subscription.repository.OutboxEventRepository;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionProductPropertyRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.ejada.subscription.repository.SubscriptionUpdateEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class MarketplaceCallbackOrchestratorTest {

    @Mock private SubscriptionRepository subscriptionRepo;
    @Mock private SubscriptionFeatureRepository featureRepo;
    @Mock private SubscriptionAdditionalServiceRepository additionalServiceRepo;
    @Mock private SubscriptionProductPropertyRepository propertyRepo;
    @Mock private SubscriptionEnvironmentIdentifierRepository envIdRepo;
    @Mock private InboundNotificationAuditRepository auditRepo;
    @Mock private SubscriptionUpdateEventRepository updateEventRepo;
    @Mock private OutboxEventRepository outboxRepo;
    @Mock private IdempotentRequestRepository idemRepo;
    @Mock private SubscriptionMapper subscriptionMapper;
    @Mock private SubscriptionFeatureMapper featureMapper;
    @Mock private SubscriptionAdditionalServiceMapper additionalServiceMapper;
    @Mock private SubscriptionProductPropertyMapper propertyMapper;
    @Mock private SubscriptionEnvironmentIdentifierMapper envIdMapper;
    @Mock private SubscriptionUpdateEventMapper updateEventMapper;
    @Mock private SubscriptionApprovalPublisher approvalPublisher;

    private MarketplaceCallbackOrchestrator orchestrator;
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new NoOpTransactionManager();
        orchestrator = new MarketplaceCallbackOrchestrator(
                subscriptionRepo,
                featureRepo,
                additionalServiceRepo,
                propertyRepo,
                envIdRepo,
                auditRepo,
                updateEventRepo,
                outboxRepo,
                idemRepo,
                subscriptionMapper,
                featureMapper,
                additionalServiceMapper,
                propertyMapper,
                envIdMapper,
                updateEventMapper,
                new ObjectMapper(),
                transactionManager,
                approvalPublisher,
                new com.ejada.subscription.tenant.TenantLinkFactory());
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
        when(auditRepo.save(any())).thenReturn(savedAudit);
        when(auditRepo.markProcessed(any(), any(), any(), any())).thenReturn(1);
        when(auditRepo.findByRqUidAndEndpoint(rqUid, "RECEIVE_NOTIFICATION")).thenReturn(Optional.empty());

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
        when(envIdRepo.findBySubscriptionSubscriptionId(200L)).thenReturn(List.of());
        when(envIdMapper.toDtoList(List.of())).thenReturn(List.of());
        when(idemRepo.existsByIdempotencyKey(rqUid)).thenReturn(false);

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                orchestrator.processNotification(rqUid, "token", request);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<com.ejada.subscription.tenant.TenantLink> linkCaptor =
                ArgumentCaptor.forClass(com.ejada.subscription.tenant.TenantLink.class);
        verify(approvalPublisher)
                .publishApprovalDecision(
                        eq(SubscriptionApprovalAction.APPROVED),
                        eq(rqUid),
                        eq(request),
                        any(Subscription.class),
                        linkCaptor.capture());

        com.ejada.subscription.tenant.TenantLink link = linkCaptor.getValue();
        assertThat(link.tenantCode()).isEqualTo("CUST-456");
        assertThat(link.securityTenantId()).isNotNull();
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

        InboundNotificationAudit audit = new InboundNotificationAudit();
        audit.setProcessed(Boolean.TRUE);
        when(auditRepo.findByRqUidAndEndpoint(rqUid, "RECEIVE_NOTIFICATION")).thenReturn(Optional.of(audit));

        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(321L);
        subscription.setExtSubscriptionId(123L);
        subscription.setExtCustomerId(456L);
        when(subscriptionRepo.findByExtSubscriptionIdAndExtCustomerId(123L, 456L))
                .thenReturn(Optional.of(subscription));
        when(envIdRepo.findBySubscriptionSubscriptionId(321L)).thenReturn(List.of());
        when(envIdMapper.toDtoList(List.of())).thenReturn(List.of());

        ServiceResult<ReceiveSubscriptionNotificationRs> result =
                orchestrator.processNotification(rqUid, "token", request);

        assertThat(result.statusCode()).isEqualTo("I000000");
        verify(auditRepo, never()).save(any());
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void processUpdateTransitionsStatusAndHandlesOutboxFailure() {
        UUID rqUid = UUID.randomUUID();
        ReceiveSubscriptionUpdateRq request = new ReceiveSubscriptionUpdateRq(
                123L, 456L, SubscriptionUpdateType.TERMINATED);

        when(updateEventRepo.findByRqUid(rqUid)).thenReturn(Optional.empty());

        InboundNotificationAudit savedAudit = new InboundNotificationAudit();
        savedAudit.setInboundNotificationAuditId(10L);
        when(auditRepo.save(any())).thenReturn(savedAudit);
        when(auditRepo.markProcessed(any(), any(), any(), any())).thenReturn(1);

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

        doThrow(new RuntimeException("outbox down")).when(outboxRepo).save(any());
        when(idemRepo.existsByIdempotencyKey(rqUid)).thenReturn(false);

        ServiceResult<Void> result = orchestrator.processUpdate(rqUid, "token", request);

        assertThat(result.statusCode()).isEqualTo("I000000");
        assertThat(subscription.getSubscriptionSttsCd()).isEqualTo("CANCELED");
        assertThat(subscription.getIsDeleted()).isTrue();
        verify(auditRepo).markProcessed(eq(10L), eq("I000000"), eq("Successful Operation"), eq(null));
        verify(outboxRepo).save(any());
    }

    private static final class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            // no-op
        }

        @Override
        public void rollback(TransactionStatus status) {
            // no-op
        }
    }
}
