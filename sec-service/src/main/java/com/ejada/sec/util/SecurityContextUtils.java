package com.ejada.sec.util;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility methods for extracting details from the Spring Security context.
 */
public final class SecurityContextUtils {

    private SecurityContextUtils() {
    }

    /**
     * Resolve the current authenticated user's identifier.
     *
     * @return user identifier as {@link Long}
     * @throws AuthenticationCredentialsNotFoundException if no authenticated user is present
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();

        Long fromPrincipal = resolveIdFromPrincipal(principal);
        if (fromPrincipal != null) {
            return fromPrincipal;
        }

        Long fromName = parseLong(authentication.getName());
        if (fromName != null) {
            return fromName;
        }

        throw new AuthenticationCredentialsNotFoundException("Unable to resolve current user identifier");
    }

    private static Long resolveIdFromPrincipal(Object principal) {
        if (principal == null) {
            return null;
        }

        if (principal instanceof Long value) {
            return value;
        }

        if (principal instanceof Integer intValue) {
            return intValue.longValue();
        }

        if (principal instanceof Jwt jwt) {
            Long id = extractLongClaim(jwt, "uid");
            if (id != null) {
                return id;
            }
            id = extractLongClaim(jwt, "user_id");
            if (id != null) {
                return id;
            }
            return parseLong(jwt.getSubject());
        }

        if (principal instanceof UserDetails details) {
            return parseLong(details.getUsername());
        }

        if (principal instanceof String str) {
            return parseLong(str);
        }

        return null;
    }

    private static Long extractLongClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);
        if (claim == null) {
            return null;
        }

        if (claim instanceof Number number) {
            return number.longValue();
        }

        if (claim instanceof String str) {
            return parseLong(str);
        }

        return null;
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
