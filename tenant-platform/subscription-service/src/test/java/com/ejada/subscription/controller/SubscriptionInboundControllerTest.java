package com.ejada.subscription.controller;

import static com.ejada.testsupport.assertions.ResponseAssertions.assertThatServiceResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.ServiceResult;
import com.ejada.subscription.dto.AdminUserInfoDto;
import com.ejada.subscription.dto.CustomerInfoDto;
import com.ejada.subscription.dto.EnvironmentIdentifierDto;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRq;
import com.ejada.subscription.dto.ReceiveSubscriptionNotificationRs;
import com.ejada.subscription.dto.ReceiveSubscriptionUpdateRq;
import com.ejada.subscription.dto.SubscriptionInfoDto;
import com.ejada.subscription.dto.SubscriptionUpdateType;
import com.ejada.subscription.exception.ServiceResultException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SubscriptionInboundControllerTest {

    @Mock
    private SubscriptionInboundService service;

    private SubscriptionInboundController controller;

    @BeforeEach
    void setUp() {
        controller = new SubscriptionInboundController(service);
    }

    @Test
    void receiveSubscriptionNotificationReturnsOkWhenServiceSucceeds() {
        ReceiveSubscriptionNotificationRq request = notificationRequest();
        ReceiveSubscriptionNotificationRs payload =
                new ReceiveSubscriptionNotificationRs(Boolean.TRUE, List.of(new EnvironmentIdentifierDto("DB_ID", "10.0.0.1")));
        when(service.receiveSubscriptionNotification(any(), any(), eq(request)))
                .thenReturn(ServiceResult.ok(payload));

        ResponseEntity<ServiceResult<ReceiveSubscriptionNotificationRs>> response =
                controller.receiveSubscriptionNotification(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatServiceResult(response.getBody())
                .isSuccess()
                .hasPayloadSatisfying(body -> assertThat(body.environmentIdentiferLst()).hasSize(1));
    }

    @Test
    void receiveSubscriptionNotificationMapsServiceFailureToBadRequest() {
        ReceiveSubscriptionNotificationRq request = notificationRequest();
        ServiceResult<ReceiveSubscriptionNotificationRs> failure =
                ServiceResult.error(null, "EVAL100", "Validation failed", List.of("missing tenant"));
        when(service.receiveSubscriptionNotification(any(), any(), eq(request))).thenReturn(failure);

        ResponseEntity<ServiceResult<ReceiveSubscriptionNotificationRs>> response =
                controller.receiveSubscriptionNotification(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatServiceResult(response.getBody())
                .isFailure()
                .hasStatusCode("EVAL100");
    }

    @Test
    void receiveSubscriptionNotificationMapsServiceExceptionToHttpStatusFromResult() {
        ReceiveSubscriptionNotificationRq request = notificationRequest();
        ServiceResult<ReceiveSubscriptionNotificationRs> failure =
                ServiceResult.error(null, "EINT000", "Unexpected", List.of("boom"));
        doThrow(new ServiceResultException(failure))
                .when(service)
                .receiveSubscriptionNotification(any(), any(), eq(request));

        ResponseEntity<ServiceResult<ReceiveSubscriptionNotificationRs>> response =
                controller.receiveSubscriptionNotification(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThatServiceResult(response.getBody())
                .isFailure()
                .hasStatusCode("EINT000");
    }

    @Test
    void receiveSubscriptionUpdateReturnsStatusFromServiceResult() {
        ReceiveSubscriptionUpdateRq request = updateRequest();
        ServiceResult<Void> failure = ServiceResult.error(null, "EUPDATE", "invalid state", List.of("not active"));
        when(service.receiveSubscriptionUpdate(any(), any(), eq(request))).thenReturn(failure);

        ResponseEntity<ServiceResult<Void>> response =
                controller.receiveSubscriptionUpdate(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatServiceResult(response.getBody())
                .isFailure()
                .hasStatusCode("EUPDATE");
    }

    private ReceiveSubscriptionNotificationRq notificationRequest() {
        SubscriptionInfoDto subscriptionInfo = new SubscriptionInfoDto(
                11L,
                22L,
                33L,
                44L,
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
        CustomerInfoDto customer = new CustomerInfoDto("Ejada", "اجادة", null, null, null, null, null, null, "ops@example.com", "+966");
        AdminUserInfoDto admin = new AdminUserInfoDto("admin", "EN", "1234567890", "admin@example.com");
        return new ReceiveSubscriptionNotificationRq(customer, admin, subscriptionInfo, List.of());
    }

    private ReceiveSubscriptionUpdateRq updateRequest() {
        return new ReceiveSubscriptionUpdateRq(UUID.randomUUID().toString(), SubscriptionUpdateType.SUSPEND, "tenant-1", "reason");
    }
}
