package com.ejada.sec.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.tenant.TenantIdentifiers;
import com.ejada.sec.dto.UserDto;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class TenantAdminProvisioningListenerTest {

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private Acknowledgment acknowledgment;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private TenantAdminProvisioningListener listener;

    @BeforeEach
    void setUp() {
        listener = new TenantAdminProvisioningListener(
                objectMapper, userService, userRepository, roleRepository);
    }

    @Test
    void ignoresNonApprovedMessages() {
        SubscriptionApprovalMessage message = baseMessage(SubscriptionApprovalAction.REQUEST);

        listener.onMessage(asPayload(message), acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(userService, never()).create(any());
    }

    @Test
    void skipsWhenAdminEmailMissing() {
        SubscriptionApprovalMessage message =
                new SubscriptionApprovalMessage(
                        SubscriptionApprovalAction.APPROVED,
                        UUID.randomUUID(),
                        123L,
                        456L,
                        "Customer",
                        null,
                        null,
                        null,
                        "TEN-1",
                        "Tenant",
                        "ops@example.com",
                        null,
                        "role",
                        OffsetDateTime.now(),
                        null);

        listener.onMessage(asPayload(message), acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(userService, never()).create(any());
    }

    @Test
    void createsAdminUserWhenMissing() {
        SubscriptionApprovalMessage message = baseMessage(SubscriptionApprovalAction.APPROVED);
        UUID tenantId = TenantIdentifiers.deriveTenantId(message.tenantCode());

        when(roleRepository.existsByTenantIdAndCode(tenantId, "TENANT_ADMIN")).thenReturn(false);
        when(userRepository.existsByTenantIdAndEmail(tenantId, message.adminEmail())).thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(eq(tenantId), any())).thenReturn(false);
        when(userService.create(any())).thenReturn(BaseResponse.success(
                "created",
                UserDto.builder()
                        .tenantId(tenantId)
                        .username("admin")
                        .email(message.adminEmail())
                        .build()));

        assertThatCode(() -> listener.onMessage(asPayload(message), acknowledgment)).doesNotThrowAnyException();

        verify(roleRepository).save(any());
        verify(userService).create(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void rethrowsWhenUserCreationFails() {
        SubscriptionApprovalMessage message = baseMessage(SubscriptionApprovalAction.APPROVED);
        UUID tenantId = TenantIdentifiers.deriveTenantId(message.tenantCode());

        when(roleRepository.existsByTenantIdAndCode(tenantId, "TENANT_ADMIN")).thenReturn(true);
        when(userRepository.existsByTenantIdAndEmail(tenantId, message.adminEmail())).thenReturn(false);
        when(userRepository.existsByTenantIdAndUsername(eq(tenantId), any())).thenReturn(false);
        when(userService.create(any())).thenReturn(BaseResponse.error("ERR", "boom"));

        assertThatThrownBy(() -> listener.onMessage(asPayload(message), acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create admin user");

        verify(acknowledgment, never()).acknowledge();
    }

    private SubscriptionApprovalMessage baseMessage(final SubscriptionApprovalAction action) {
        return new SubscriptionApprovalMessage(
                action,
                UUID.randomUUID(),
                123L,
                456L,
                "Customer",
                null,
                "admin@example.com",
                "+966500000000",
                "TEN-100",
                "Tenant",
                "ops@example.com",
                "+966500000001",
                "role",
                OffsetDateTime.now(),
                null);
    }

    private Map<String, Object> asPayload(final SubscriptionApprovalMessage message) {
        return objectMapper.convertValue(message, new TypeReference<Map<String, Object>>() {});
    }
}
