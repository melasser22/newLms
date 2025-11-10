package com.ejada.subscription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.subscription.dto.AdminUserInfoDto;
import com.ejada.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.ServiceResult;
import com.ejada.subscription.dto.SubscriptionInfoDto;
import com.ejada.subscription.dto.SubscriptionUpdateType;
import com.ejada.subscription.service.SubscriptionInboundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SubscriptionInboundControllerTest {

  private static final UUID RQ_UID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

  @Mock private SubscriptionInboundService service;

  private SubscriptionInboundController controller;

  @BeforeEach
  void setUp() {
    controller = new SubscriptionInboundController(service);
  }

  @Test
  void receiveSubscriptionNotificationCollapsesMultipleTokenHeaders() {
    ReceiveSubscriptionNotificationRq request = notificationRequest();
    ServiceResult<ReceiveSubscriptionNotificationRs> result =
        new ServiceResult<>("I000000", "Successful Operation", null, new ReceiveSubscriptionNotificationRs(true, List.of()));
    when(service.receiveSubscriptionNotification(eq(RQ_UID), eq("string,token"), eq(request))).thenReturn(result);

    ResponseEntity<ServiceResult<ReceiveSubscriptionNotificationRs>> response =
        controller.receiveSubscriptionNotification(RQ_UID, List.of("string", "token"), request);

    assertThat(response.getBody()).isSameAs(result);
    verify(service).receiveSubscriptionNotification(eq(RQ_UID), eq("string,token"), eq(request));
  }

  @Test
  void receiveSubscriptionUpdateCollapsesMultipleTokenHeaders() {
    ReceiveSubscriptionUpdateRq request = new ReceiveSubscriptionUpdateRq(1L, 2L, SubscriptionUpdateType.SUSPENDED);
    ServiceResult<Void> result = new ServiceResult<>("I000000", "Successful Operation", null, null);
    when(service.receiveSubscriptionUpdate(eq(RQ_UID), eq("string,token"), eq(request))).thenReturn(result);

    ResponseEntity<ServiceResult<Void>> response =
        controller.receiveSubscriptionUpdate(RQ_UID, List.of("string", "token"), request);

    assertThat(response.getBody()).isSameAs(result);
    verify(service).receiveSubscriptionUpdate(eq(RQ_UID), eq("string,token"), eq(request));
  }

  private ReceiveSubscriptionNotificationRq notificationRequest() {
    CustomerInfoDto customer =
        new CustomerInfoDto(
            "Customer",
            "العميل",
            "COMPANY",
            "123",
            "SA",
            "RYD",
            "Address",
            "العنوان",
            "customer@example.com",
            "0500000000");
    AdminUserInfoDto admin = new AdminUserInfoDto("Admin", "EN", "0500000001", "admin@example.com");
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
}
