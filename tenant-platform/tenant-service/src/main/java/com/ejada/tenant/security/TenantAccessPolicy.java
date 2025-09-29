package com.ejada.tenant.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessPolicy {

    private final Set<String> allowedRoles;

    public TenantAccessPolicy(@Value("${tenant.security.allowed-roles:EJADA_OFFICER}") final String allowedRoles) {
        if (allowedRoles == null || allowedRoles.isBlank()) {
            this.allowedRoles = Collections.emptySet();
        } else {
            this.allowedRoles = Arrays.stream(allowedRoles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        }
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
