package com.ejada.sec.security;

import com.ejada.sec.service.PermissionEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * SpEL helper that bridges Spring Security annotations with the permission service.
 */
@Component("permissionEvaluator")
@RequiredArgsConstructor
@Slf4j
public class PermissionEvaluator {

    private final PermissionEvaluationService permissionService;

    public boolean hasPermission(Authentication authentication, String privilegeCode) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Permission check failed - unauthenticated request");
            return false;
        }

        Long userId = extractUserId(authentication);
        if (userId == null) {
            log.warn("Permission check failed - userId missing in authentication token");
            return false;
        }

        return permissionService.hasPermission(userId, privilegeCode);
    }

    public boolean hasResourcePermission(Authentication authentication, String resource, String action) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Long userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return permissionService.hasResourcePermission(userId, resource, action);
    }

    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Number number) {
                return number.longValue();
            }
            if (userIdClaim instanceof String stringValue) {
                try {
                    return Long.parseLong(stringValue);
                } catch (NumberFormatException ex) {
                    log.error("Invalid userId claim format: {}", stringValue);
                }
            }
        }
        return null;
    }
}
