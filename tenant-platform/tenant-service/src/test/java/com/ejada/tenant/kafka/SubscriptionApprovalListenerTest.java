package com.ejada.tenant.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.tenant.dto.TenantCreateReq;
import com.ejada.tenant.exception.TenantConflictException;
import com.ejada.tenant.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionApprovalListenerTest {

    @Mock private TenantService tenantService;

    private final ObjectMapper objectMapper = new ObjectMapper();
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

        listener.onMessage(toPayload(message));

        verifyNoInteractions(tenantService);
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

        assertThatThrownBy(() -> listener.onMessage(toPayload(message)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing tenant details");

        verifyNoInteractions(tenantService);
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

        listener.onMessage(toPayload(message));

        ArgumentCaptor<TenantCreateReq> captor = ArgumentCaptor.forClass(TenantCreateReq.class);
        verify(tenantService).create(captor.capture());

        assertThat(captor.getValue())
                .isEqualTo(new TenantCreateReq(
                        "TEN-42",
                        "Tenant Corp",
                        "ops@example.com",
                        "+966500000001",
                        null,
                        Boolean.TRUE));
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

        when(tenantService.create(any())).thenThrow(new TenantConflictException("duplicate"));

        assertThatCode(() -> listener.onMessage(toPayload(message))).doesNotThrowAnyException();

        verify(tenantService).create(any());
    }

    private Map<String, Object> toPayload(final SubscriptionApprovalMessage message) {
        return objectMapper.convertValue(message, Map.class);
    }
}
