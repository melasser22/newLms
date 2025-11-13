package com.ejada.sec.messaging;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantAdminInfo;
import com.ejada.sec.domain.Role;
import com.ejada.sec.domain.User;
import com.ejada.sec.domain.UserRole;
import com.ejada.sec.domain.UserRoleId;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.repository.UserRoleRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAdminProvisioningService {

    private static final String TENANT_ADMIN_ROLE_CODE = "TENANT_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void provisionTenantAdmin(final TenantProvisioningEvent event) {
        if (event == null) {
            log.warn("Received null tenant provisioning event; skipping admin provisioning");
            return;
        }

        String extCustomerId = normalize(event.extCustomerId());
        if (!StringUtils.hasText(extCustomerId)) {
            log.warn("Skipping admin provisioning: missing extCustomerId in event {}", event.subscriptionId());
            return;
        }

        TenantAdminInfo adminInfo = event.adminInfo();
        if (adminInfo == null) {
            log.warn("Skipping admin provisioning for customer {}: admin info missing", extCustomerId);
            return;
        }

        String username = normalize(adminInfo.adminUserName());
        String email = normalize(adminInfo.email());
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email)) {
            log.warn("Skipping admin provisioning for customer {}: incomplete admin info", extCustomerId);
            return;
        }

        UUID internalTenantId = event.internalTenantId() != null
                ? event.internalTenantId()
                : tenantIdFromExternal(extCustomerId);
        if (internalTenantId == null) {
            log.warn("Skipping admin provisioning for customer {}: unable to determine tenant id", extCustomerId);
            return;
        }
        Optional<User> existing = userRepository.findByTenantIdAndUsername(internalTenantId, username);
        if (existing.isPresent()) {
            updateExistingAdmin(existing.get(), email, internalTenantId);
            ensureRoleAssignment(existing.get(), internalTenantId);
            return;
        }

        if (userRepository.existsByTenantIdAndEmail(internalTenantId, email)) {
            log.warn("Skipping admin creation for tenant {}: email {} already in use", internalTenantId, email);
            return;
        }

        User user = new User();
        user.setTenantId(internalTenantId);
        user.setInternalTenantId(internalTenantId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(generateRandomPassword()));
        user.setEnabled(true);
        user.setLocked(false);

        user = userRepository.save(user);
        ensureRoleAssignment(user, internalTenantId);
        log.info("Provisioned tenant admin '{}' for tenant {}", username, internalTenantId);
    }

    private void updateExistingAdmin(final User user, final String email, final UUID tenantId) {
        if (email.equalsIgnoreCase(user.getEmail())) {
            return;
        }
        userRepository.findByTenantIdAndEmail(tenantId, email)
                .filter(other -> !other.getId().equals(user.getId()))
                .ifPresentOrElse(
                        other -> log.warn(
                                "Skipping email update for user {} in tenant {}: email already used by user {}",
                                user.getId(), tenantId, other.getId()),
                        () -> {
                            user.setEmail(email);
                            userRepository.save(user);
                            log.info("Updated admin email for user {} in tenant {}", user.getId(), tenantId);
                        });
    }

    private void ensureRoleAssignment(final User user, final UUID tenantId) {
        Role role = roleRepository.findByTenantIdAndCode(tenantId, TENANT_ADMIN_ROLE_CODE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .tenantId(tenantId)
                        .code(TENANT_ADMIN_ROLE_CODE)
                        .name("Tenant Administrator")
                        .build()));

        UserRoleId id = new UserRoleId(user.getId(), role.getId());
        if (!userRoleRepository.existsById(id)) {
            userRoleRepository.save(UserRole.builder()
                    .id(id)
                    .user(user)
                    .role(role)
                    .build());
            log.info("Assigned TENANT_ADMIN role to user {} in tenant {}", user.getId(), tenantId);
        }
    }

    private UUID tenantIdFromExternal(final String extCustomerId) {
        String key = "tenant:" + extCustomerId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(final String value) {
        return value == null ? null : value.trim();
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
