package com.ejada.starter_security;

import lombok.RequiredArgsConstructor;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

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
        String configuredPrefix = StringUtils.hasText(securityProps.getRolePrefix())
            ? securityProps.getRolePrefix()
            : "ROLE_";

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }

        for (GrantedAuthority authority : authorities) {
            String authorityValue = authority != null ? authority.getAuthority() : null;
            if (!StringUtils.hasText(authorityValue)) {
                continue;
            }

            String normalizedAuthority = normalizeAuthority(authorityValue, configuredPrefix);

            for (Role role : roles) {
                if (role == null) {
                    continue;
                }

                String expectedAuthority = role.getAuthority();
                if (authorityValue.equals(expectedAuthority)) {
                    return true;
                }

                if (normalizedAuthority.equals(role.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String normalizeAuthority(String authority, String configuredPrefix) {
        if (!StringUtils.hasText(authority)) {
            return "";
        }

        if (StringUtils.hasText(configuredPrefix) && authority.startsWith(configuredPrefix)) {
            return authority.substring(configuredPrefix.length());
        }

        if (authority.startsWith("ROLE_")) {
            return authority.substring("ROLE_".length());
        }

        return authority;
    }
}
