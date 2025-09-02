package com.ejada.starter_security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Evaluates whether the current user satisfies the required role.
 */
@RequiredArgsConstructor
public class RoleChecker {

    private final SharedSecurityProps securityProps;

    /**
     * Returns true when role checks are disabled or the authenticated user has at least one of the provided roles.
     *
     * @param authentication current authentication
     * @param roles required roles
     * @return whether access should be granted
     */
    public boolean hasRole(Authentication authentication, Role... roles) {
        if (!securityProps.isEnableRoleCheck()) {
            return true;
        }
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (Role role : roles) {
                if (role.getAuthority().equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}
