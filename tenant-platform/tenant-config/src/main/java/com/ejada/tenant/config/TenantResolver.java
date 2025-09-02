package com.ejada.tenant.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

/**
 * Resolves tenant identifiers from incoming requests.
 */
public class TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT_CLAIM = "tenant_id";

    /**
     * Resolve the tenant identifier from the request or security context.
     *
     * @param request incoming request
     * @return optional tenant identifier
     */
    public Optional<UUID> resolveTenant(HttpServletRequest request) {
        String headerValue = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(headerValue)) {
            return parse(headerValue);
        }
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                String claim = jwtAuthenticationToken.getToken().getClaimAsString(TENANT_CLAIM);
                if (StringUtils.hasText(claim)) {
                    return parse(claim);
                }
            }
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                String claim = jwt.getClaimAsString(TENANT_CLAIM);
                if (StringUtils.hasText(claim)) {
                    return parse(claim);
                }
            }
        } catch (NoClassDefFoundError ignored) {
            // Spring Security not present
        }
        return Optional.empty();
    }

    private Optional<UUID> parse(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
