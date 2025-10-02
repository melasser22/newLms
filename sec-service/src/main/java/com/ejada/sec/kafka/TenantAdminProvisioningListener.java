package com.ejada.sec.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.common.tenant.TenantIdentifiers;
import com.ejada.sec.dto.CreateUserRequest;
import com.ejada.sec.dto.UserDto;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAdminProvisioningListener {

    private static final List<String> ADMIN_ROLES = List.of("TENANT_ADMIN");
    private static final int MAX_USERNAME_LENGTH = 120;
    private static final int GENERATED_PASSWORD_LENGTH = 16;
    private static final char[] PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#%&*!".toCharArray();

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @KafkaListener(
            topics = "#{@subscriptionApprovalProperties.topic}",
            groupId = "#{@subscriptionApprovalProperties.consumerGroup}",
            containerFactory = "subscriptionApprovalListenerContainerFactory")
    @Transactional
    public void onMessage(@Payload final Map<String, Object> payload, final Acknowledgment acknowledgment) {
        SubscriptionApprovalMessage message = objectMapper.convertValue(payload, SubscriptionApprovalMessage.class);

        if (message.action() != SubscriptionApprovalAction.APPROVED) {
            acknowledgment.acknowledge();
            return;
        }

        if (!StringUtils.hasText(message.tenantCode())) {
            throw new IllegalArgumentException("Missing tenant code in approval message " + message.requestId());
        }

        if (!StringUtils.hasText(message.adminEmail())) {
            log.warn(
                    "Subscription approval {} for tenant {} does not include admin email; skipping auto provisioning",
                    message.requestId(),
                    message.tenantCode());
            acknowledgment.acknowledge();
            return;
        }

        UUID tenantId = TenantIdentifiers.deriveTenantId(message.tenantCode());
        ensureTenantAdminRoleExists(tenantId);

        if (userRepository.existsByTenantIdAndEmail(tenantId, message.adminEmail())) {
            log.info(
                    "Admin user {} already exists for tenant {}", message.adminEmail(), message.tenantCode());
            acknowledgment.acknowledge();
            return;
        }

        String username = generateUsername(tenantId, message);
        String password = generateTemporaryPassword();

        CreateUserRequest request = CreateUserRequest.builder()
                .tenantId(tenantId)
                .username(username)
                .email(message.adminEmail())
                .password(password)
                .roles(ADMIN_ROLES)
                .build();

        var response = userService.create(request);
        if (!response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException(
                    "Failed to create admin user for tenant %s: %s".formatted(
                            message.tenantCode(), response.getCode()));
        }

        UserDto created = response.getData();
        log.info(
                "Provisioned tenant admin '{}' ({}) for tenant {}. Temporary credentials issued; enforce password reset.",
                created.getUsername(),
                created.getEmail(),
                message.tenantCode());

        acknowledgment.acknowledge();
    }

    private void ensureTenantAdminRoleExists(final UUID tenantId) {
        if (roleRepository.existsByTenantIdAndCode(tenantId, ADMIN_ROLES.getFirst())) {
            return;
        }

        var role = new com.ejada.sec.domain.Role();
        role.setTenantId(tenantId);
        role.setCode(ADMIN_ROLES.getFirst());
        role.setName("Tenant Administrator");
        roleRepository.save(role);
    }

    private String generateUsername(final UUID tenantId, final SubscriptionApprovalMessage message) {
        String base = null;
        if (StringUtils.hasText(message.adminEmail())) {
            int at = message.adminEmail().indexOf('@');
            base = at > 0 ? message.adminEmail().substring(0, at) : message.adminEmail();
        }
        if (!StringUtils.hasText(base) && StringUtils.hasText(message.tenantCode())) {
            base = message.tenantCode().toLowerCase(Locale.ROOT) + ".admin";
        }
        if (!StringUtils.hasText(base)) {
            base = "tenant.admin";
        }

        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
        base = base.length() > MAX_USERNAME_LENGTH ? base.substring(0, MAX_USERNAME_LENGTH) : base;

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByTenantIdAndUsername(tenantId, candidate)) {
            String suffixStr = String.valueOf(suffix++);
            int maxLength = MAX_USERNAME_LENGTH - suffixStr.length();
            String trimmed = candidate.length() > maxLength ? candidate.substring(0, maxLength) : candidate;
            candidate = trimmed + suffixStr;
        }
        return candidate;
    }

    private String generateTemporaryPassword() {
        char[] buffer = new char[GENERATED_PASSWORD_LENGTH];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = PASSWORD_ALPHABET[secureRandom.nextInt(PASSWORD_ALPHABET.length)];
        }
        return new String(buffer);
    }
}
