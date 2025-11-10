package com.ejada.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ejada.subscription.dto.AdminUserInfoDto;
import com.ejada.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.ServiceResult;
import com.ejada.subscription.dto.SubscriptionInfoDto;
import com.ejada.subscription.dto.SubscriptionUpdateType;
import com.ejada.subscription.mapper.SubscriptionAdditionalServiceMapper;
import com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper;
import com.ejada.subscription.mapper.SubscriptionFeatureMapper;
import com.ejada.subscription.mapper.SubscriptionMapper;
import com.ejada.subscription.mapper.SubscriptionProductPropertyMapper;
import com.ejada.subscription.mapper.SubscriptionUpdateEventMapper;
import com.ejada.subscription.model.InboundNotificationAudit;
import com.ejada.subscription.repository.IdempotentRequestRepository;
import com.ejada.subscription.repository.InboundNotificationAuditRepository;
import com.ejada.subscription.repository.OutboxEventRepository;
import com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository;
import com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository;
import com.ejada.subscription.repository.SubscriptionFeatureRepository;
import com.ejada.subscription.repository.SubscriptionProductPropertyRepository;
import com.ejada.subscription.repository.SubscriptionRepository;
import com.ejada.subscription.repository.SubscriptionUpdateEventRepository;
import com.ejada.subscription.security.JwtValidator;
import com.ejada.subscription.service.impl.SubscriptionInboundServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionInboundServiceImplTest {

  private static final UUID RQ_UID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private SubscriptionFeatureRepository featureRepository;
  @Mock private SubscriptionAdditionalServiceRepository additionalServiceRepository;
  @Mock private SubscriptionProductPropertyRepository productPropertyRepository;
  @Mock private SubscriptionEnvironmentIdentifierRepository environmentIdentifierRepository;
  @Mock private InboundNotificationAuditRepository auditRepository;
  @Mock private SubscriptionUpdateEventRepository updateEventRepository;
  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private IdempotentRequestRepository idempotentRequestRepository;
  @Mock private SubscriptionMapper subscriptionMapper;
  @Mock private SubscriptionFeatureMapper subscriptionFeatureMapper;
  @Mock private SubscriptionAdditionalServiceMapper subscriptionAdditionalServiceMapper;
  @Mock private SubscriptionProductPropertyMapper subscriptionProductPropertyMapper;
  @Mock private SubscriptionEnvironmentIdentifierMapper subscriptionEnvironmentIdentifierMapper;
  @Mock private SubscriptionUpdateEventMapper subscriptionUpdateEventMapper;
  @Mock private JwtValidator jwtValidator;

  private SubscriptionInboundServiceImpl service;

  @BeforeEach
  void setUp() {
    when(auditRepository.save(any())).thenAnswer(invocation -> {
      InboundNotificationAudit entity = invocation.getArgument(0, InboundNotificationAudit.class);
      entity.setInboundNotificationAuditId(42L);
      return entity;
    });
    when(auditRepository.markProcessed(anyLong(), any(), any(), any())).thenReturn(1);

    service =
        new SubscriptionInboundServiceImpl(
            subscriptionRepository,
            featureRepository,
            additionalServiceRepository,
            productPropertyRepository,
            environmentIdentifierRepository,
            auditRepository,
            updateEventRepository,
            outboxEventRepository,
            idempotentRequestRepository,
            subscriptionMapper,
            subscriptionFeatureMapper,
            subscriptionAdditionalServiceMapper,
            subscriptionProductPropertyMapper,
            subscriptionEnvironmentIdentifierMapper,
            subscriptionUpdateEventMapper,
            new ObjectMapper(),
            jwtValidator);
  }

  @Test
  void receiveSubscriptionNotificationRejectsInvalidToken() {
    when(jwtValidator.isValid("bad-token")).thenReturn(false);

    ReceiveSubscriptionNotificationRq request = notificationRequest();

    ServiceResult<ReceiveSubscriptionNotificationRs> result =
        service.receiveSubscriptionNotification(RQ_UID, "bad-token", request);

    assertThat(result.statusCode()).isEqualTo("ESEC401");
    assertThat(result.statusDescription()).isEqualTo("Unauthorized");
    assertThat(result.statusDetails()).isEqualTo("{\"message\":\"invalid or expired token\"}");
    assertThat(result.payload()).isNull();

    verify(jwtValidator).isValid("bad-token");
    verify(auditRepository).save(any(InboundNotificationAudit.class));
    verify(auditRepository)
        .markProcessed(eq(42L), eq("ESEC401"), eq("Unauthorized"), eq("{\"message\":\"invalid or expired token\"}"));

    verifyNoFurtherInteractionsWithBusinessDependencies();
  }

  @Test
  void receiveSubscriptionNotificationNormalizesBearerToken() {
    String jwt = "header.payload.signature";
    when(jwtValidator.isValid(jwt)).thenReturn(false);

    ReceiveSubscriptionNotificationRq request = notificationRequest();

    service.receiveSubscriptionNotification(RQ_UID, "Bearer " + jwt, request);

    verify(jwtValidator).isValid(jwt);
    ArgumentCaptor<InboundNotificationAudit> auditCaptor =
        ArgumentCaptor.forClass(InboundNotificationAudit.class);
    verify(auditRepository).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getTokenHash()).isEqualTo(sha256(jwt));
  }

  @Test
  void receiveSubscriptionNotificationExtractsTokenFromCommaSeparatedHeader() {
    String jwt = "header.payload.signature";
    when(jwtValidator.isValid(jwt)).thenReturn(false);

    ReceiveSubscriptionNotificationRq request = notificationRequest();

    service.receiveSubscriptionNotification(RQ_UID, "placeholder, Bearer " + jwt, request);

    verify(jwtValidator).isValid(jwt);
    ArgumentCaptor<InboundNotificationAudit> auditCaptor =
        ArgumentCaptor.forClass(InboundNotificationAudit.class);
    verify(auditRepository).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getTokenHash()).isEqualTo(sha256(jwt));
  }

  @Test
  void receiveSubscriptionUpdateRejectsInvalidToken() {
    when(jwtValidator.isValid(null)).thenReturn(false);

    ReceiveSubscriptionUpdateRq request = new ReceiveSubscriptionUpdateRq(1L, 2L, SubscriptionUpdateType.SUSPENDED);

    ServiceResult<Void> result = service.receiveSubscriptionUpdate(RQ_UID, null, request);

    assertThat(result.statusCode()).isEqualTo("ESEC401");
    assertThat(result.statusDescription()).isEqualTo("Unauthorized");
    assertThat(result.statusDetails()).isEqualTo("{\"message\":\"invalid or expired token\"}");
    assertThat(result.payload()).isNull();

    verify(jwtValidator).isValid(null);
    verify(auditRepository).save(any(InboundNotificationAudit.class));
    verify(auditRepository)
        .markProcessed(eq(42L), eq("ESEC401"), eq("Unauthorized"), eq("{\"message\":\"invalid or expired token\"}"));

    verifyNoFurtherInteractionsWithBusinessDependencies();
  }

  private ReceiveSubscriptionNotificationRq notificationRequest() {
    CustomerInfoDto customer =
        new CustomerInfoDto(
            "Customer", "العميل", "COMPANY", "123", "SA", "RYD", "Address", "العنوان", "customer@example.com", "0500000000");
    AdminUserInfoDto admin =
        new AdminUserInfoDto("Admin", "EN", "0500000001", "admin@example.com");
    SubscriptionInfoDto subscriptionInfo =
        new SubscriptionInfoDto(
            10L,
            20L,
            30L,
            40L,
            "Gold",
            "ذهبي",
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "ACTIVE",
            "PORTAL",
            Boolean.TRUE,
            100L,
            "FULL_SUBSCRIPTION_PERIOD",
            Boolean.FALSE,
            200L,
            "MONTHLY",
            BigDecimal.TEN,
            "MONTHLY",
            "L",
            Boolean.TRUE,
            null,
            null,
            List.of(),
            List.of());

    return new ReceiveSubscriptionNotificationRq(customer, admin, subscriptionInfo, List.of());
  }

  private void verifyNoFurtherInteractionsWithBusinessDependencies() {
    verifyNoInteractions(
        subscriptionRepository,
        featureRepository,
        additionalServiceRepository,
        productPropertyRepository,
        environmentIdentifierRepository,
        updateEventRepository,
        outboxEventRepository,
        idempotentRequestRepository,
        subscriptionMapper,
        subscriptionFeatureMapper,
        subscriptionAdditionalServiceMapper,
        subscriptionProductPropertyMapper,
        subscriptionEnvironmentIdentifierMapper,
        subscriptionUpdateEventMapper);
  }

  private String sha256(final String value) {
    if (value == null) {
      return null;
    }
    try {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
