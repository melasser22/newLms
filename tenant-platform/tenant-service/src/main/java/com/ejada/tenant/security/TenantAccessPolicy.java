package com.ejada.tenant.security;

import com.ejada.starter_security.SharedSecurityProps;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantAccessPolicy {

    private final Set<String> allowedRoles;

    public TenantAccessPolicy(
            @Value("${tenant.security.allowed-roles:ROLE_EJADA_OFFICER}") final String allowedRoles,
            final SharedSecurityProps securityProps) {
        String prefix = Optional.ofNullable(securityProps)
                .map(SharedSecurityProps::getRolePrefix)
                .orElse("ROLE_");

        String[] tokens = StringUtils.hasText(allowedRoles)
                ? StringUtils.tokenizeToStringArray(allowedRoles, ",")
                : new String[0];
        if (tokens.length == 0) {
            this.allowedRoles = Collections.emptySet();
        } else {
            this.allowedRoles = Arrays.stream(tokens)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(role -> normalizeRole(role, prefix))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private static String normalizeRole(final String configuredRole, final String prefix) {
        String trimmed = configuredRole.trim();
        if (!StringUtils.hasText(prefix)) {
            if (trimmed.startsWith("ROLE_")) {
                String withoutDefault = trimmed.substring("ROLE_".length());
                return withoutDefault.isBlank() ? trimmed : withoutDefault;
            }
            return trimmed;
        }
        if (trimmed.startsWith(prefix)) {
            return trimmed;
        }
        return prefix + trimmed;
    }

    public boolean isAllowed(final Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowedRoles::contains);
    }

    Set<String> getAllowedRoles() {
        return allowedRoles;
    }
}
