package com.ejada.starter_security.authorization;

import com.ejada.starter_security.Role;
import com.ejada.starter_security.RoleChecker;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

/**
 * Reusable security expressions that can be referenced from Spring Expression Language (SpEL)
 * {@code @PreAuthorize} declarations. Centralising these helpers keeps the
 * composed annotations concise while providing an easy extension point for
 * service applications that need to evaluate role combinations in tests.
 */
public class AuthorizationExpressions {

    private final RoleChecker roleChecker;

    public AuthorizationExpressions(RoleChecker roleChecker) {
        Assert.notNull(roleChecker, "roleChecker must not be null");
        this.roleChecker = roleChecker;
    }

    /**
     * Evaluates whether the authenticated principal has at least one of the supplied roles.
     *
     * @param authentication current authentication
     * @param roles roles that grant access
     * @return {@code true} when access should be granted
     */
    public boolean hasAnyRole(Authentication authentication, Role... roles) {
        return roleChecker.hasRole(authentication, roles);
    }

    /**
     * @return {@code true} when the current user is registered as an EJADA officer.
     */
    public boolean isEjadaOfficer(Authentication authentication) {
        return hasAnyRole(authentication, Role.EJADA_OFFICER);
    }

    /**
     * @return {@code true} when the current user belongs to the EJADA platform staff cohort.
     */
    public boolean isPlatformStaff(Authentication authentication) {
        return hasAnyRole(authentication,
                Role.EJADA_OFFICER,
                Role.TENANT_ADMIN,
                Role.TENANT_OFFICER);
    }
}
