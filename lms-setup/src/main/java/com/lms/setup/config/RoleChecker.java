package com.lms.setup.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Evaluates whether the current user satisfies the required role.
 */
@Component("roleChecker")
@RequiredArgsConstructor
public class RoleChecker {

    private final SetupSecurityProperties securityProperties;

    /**
     * Returns true when role checks are disabled or the authenticated user has the EJADA_Officer role.
     *
     * @param authentication current authentication
     * @return whether access should be granted
     */
    public boolean hasEjadaOfficerRole(Authentication authentication) {
        if (!securityProperties.isEnableRoleCheck()) {
            return true;
        }
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EJADA_Officer".equals(a.getAuthority()));
    }
}
