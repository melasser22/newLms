package com.ejada.tenant.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.tenant.TenantIdentifiers;
import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.exception.TenantConflictException;
import com.ejada.tenant.exception.TenantErrorCode;
import com.ejada.tenant.service.TenantService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class SubscriptionApprovalListenerTest {

    @Mock private TenantService tenantService;
    @Mock private Acknowledgment acknowledgment;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private SubscriptionApprovalListener listener;

    @BeforeEach
    void setUp() {
        listener = new SubscriptionApprovalListener(objectMapper, tenantService);
    }

    @Test
    void ignoresNonApprovedMessages() {
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.REQUEST,
                UUID.randomUUID(),
                123L,
                456L,
                "Customer EN",
                "Customer AR",
                "admin@example.com",
                "+966500000000",
                "TEN-1",
                "Tenant",
                "ops@example.com",
                "+966500000001",
                "role",
                OffsetDateTime.now(),
                null);

        listener.onMessage(toPayload(message), acknowledgment);

        verifyNoInteractions(tenantService);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void throwsWhenTenantDetailsMissing() {
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                UUID.randomUUID(),
                123L,
                456L,
                "Customer EN",
                "Customer AR",
                "admin@example.com",
                "+966500000000",
                null,
                "",
                "ops@example.com",
                "+966500000001",
                "role",
                OffsetDateTime.now(),
                null);

        assertThatThrownBy(() -> listener.onMessage(toPayload(message), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing tenant details");

        verifyNoInteractions(tenantService);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void provisionsTenantWhenApproved() {
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                UUID.randomUUID(),
                123L,
                456L,
                "Customer EN",
                "Customer AR",
                "admin@example.com",
                "+966500000000",
                "TEN-42",
                "Tenant Corp",
                "ops@example.com",
                "+966500000001",
                "role",
                OffsetDateTime.now(),
                null);

        listener.onMessage(toPayload(message), acknowledgment);

        ArgumentCaptor<TenantCreateReq> captor = ArgumentCaptor.forClass(TenantCreateReq.class);
        verify(tenantService).create(captor.capture());
        verify(acknowledgment).acknowledge();

        assertThat(captor.getValue())
                .isEqualTo(new TenantCreateReq(
                        "TEN-42",
                        "Tenant Corp",
                        "ops@example.com",
                        "+966500000001",
                        null,
                        Boolean.TRUE,
                        TenantIdentifiers.deriveTenantId("TEN-42")));
    }

    @Test
    void swallowsTenantConflict() {
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                UUID.randomUUID(),
                123L,
                456L,
                "Customer EN",
                "Customer AR",
                "admin@example.com",
                "+966500000000",
                "TEN-42",
                "Tenant Corp",
                "ops@example.com",
                "+966500000001",
                "role",
                OffsetDateTime.now(),
                null);

        when(tenantService.create(any()))
                .thenThrow(new TenantConflictException(TenantErrorCode.CODE_EXISTS, "duplicate"));

        assertThatCode(() -> listener.onMessage(toPayload(message), acknowledgment))
                .doesNotThrowAnyException();

        verify(tenantService).create(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void rethrowsUnexpectedFailuresForRetryHandling() {
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                UUID.randomUUID(),
                123L,
                456L,
                "Customer EN",
                "Customer AR",
                "admin@example.com",
                "+966500000000",
                "TEN-42",
                "Tenant Corp",
                "ops@example.com",
                "+966500000001",
                "role",
                OffsetDateTime.now(),
                null);

        when(tenantService.create(any())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> listener.onMessage(toPayload(message), acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        verify(tenantService).create(any());
        verify(acknowledgment, never()).acknowledge();
    }

    private Map<String, Object> toPayload(final SubscriptionApprovalMessage message) {
        return objectMapper.convertValue(message, new TypeReference<Map<String, Object>>() {});
    }
}
