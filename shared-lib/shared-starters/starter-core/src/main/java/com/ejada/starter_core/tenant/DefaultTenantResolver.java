package com.ejada.starter_core.tenant;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_core.config.CoreAutoConfiguration.CoreProps;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production tenant resolver capable of resolving tenants from JWTs, headers,
 * subdomains and administrative paths. The resolver validates every resolved
 * tenant against the provided {@link TenantDirectory} before returning it to
 * the {@link TenantContextContributor}.
 */
public class DefaultTenantResolver implements TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultTenantResolver.class);

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADMIN_PATH_PATTERN = Pattern.compile("/api/v\\d+/admin/tenants/([0-9a-fA-F-]{36})");

    private final CoreProps.Tenant cfg;
    private final TenantDirectory directory;
    private final ConcurrentMap<String, CachedTenant> subdomainCache;
    private final Duration subdomainCacheTtl;
    private final Set<String> reservedSubdomains;

    public DefaultTenantResolver(CoreProps.Tenant cfg, TenantDirectory directory) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        this.directory = Objects.requireNonNull(directory, "directory");
        this.subdomainCache = new ConcurrentHashMap<>();
        long ttlSeconds = Math.max(0, cfg.getSubdomainCacheTtlSeconds());
        this.subdomainCacheTtl = ttlSeconds == 0 ? Duration.ZERO : Duration.ofSeconds(ttlSeconds);
        this.reservedSubdomains = Arrays.stream(Optional.ofNullable(cfg.getReservedSubdomains()).orElse(new String[0]))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public TenantResolution resolve(HttpServletRequest request) {
        if (request == null) {
            return TenantResolution.absent();
        }

        TenantResolution[] attempts = new TenantResolution[] {
                resolveFromJwt(),
                resolveFromHeader(request),
                resolveFromSubdomain(request),
                resolveFromAdminPath(request)
        };

        for (TenantResolution attempt : attempts) {
            if (attempt == null || attempt.isAbsent()) {
                continue;
            }
            if (attempt.hasTenant()) {
                logResolvedTenant(attempt, request);
                return attempt;
            }
            if (attempt.hasError()) {
                logResolutionError(attempt, request);
                return attempt;
            }
        }

        if (cfg.isFailIfTenantMissing()) {
            TenantResolution failure = TenantResolution.error(tenantMissingError(), null, TenantSource.NONE);
            logResolutionError(failure, request);
            return failure;
        }

        log.debug("Tenant resolution skipped for {} {}", request.getMethod(), safePath(request));
        return TenantResolution.absent();
    }

    private TenantResolution resolveFromJwt() {
        if (!cfg.isResolveFromJwt()) {
            return TenantResolution.absent();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return TenantResolution.absent();
        }

        Map<String, Object> claims = extractJwtClaims(authentication);
        String idClaim = firstClaim(claims, cfg.getJwtClaimNames());
        if (StringUtils.hasText(idClaim)) {
            TenantResolution byId = resolveByIdCandidate(idClaim, TenantSource.JWT);
            if (!byId.isAbsent()) {
                return byId;
            }
        }

        String slugClaimName = cfg.getJwtTenantSlugClaim();
        if (StringUtils.hasText(slugClaimName)) {
            String slugClaim = trimToNull(asString(claims.get(slugClaimName)));
            if (StringUtils.hasText(slugClaim)) {
                return resolveBySlugCandidate(slugClaim, TenantSource.JWT);
            }
        }

        return TenantResolution.absent();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractJwtClaims(Authentication authentication) {
        if (authentication == null) {
            return Map.of();
        }
        try {
            Class<?> jwtAuthClass = Class.forName("org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken");
            if (!jwtAuthClass.isInstance(authentication)) {
                return Map.of();
            }
            Object jwtToken = jwtAuthClass.getMethod("getToken").invoke(authentication);
            if (jwtToken == null) {
                return Map.of();
            }
            Object claims = jwtToken.getClass().getMethod("getClaims").invoke(jwtToken);
            if (claims instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (ClassNotFoundException ex) {
            log.debug("JWT authentication class not on classpath; skipping claim extraction");
        } catch (ReflectiveOperationException ex) {
            log.warn("Failed to extract JWT claims for tenant resolution", ex);
        }
        return Map.of();
    }

    private TenantResolution resolveFromHeader(HttpServletRequest request) {
        if (!cfg.isAllowHeaderResolution()) {
            return TenantResolution.absent();
        }

        String headerName = cfg.getHeaderName();
        String headerValue = trimToNull(request.getHeader(headerName));
        if (headerValue == null) {
            return TenantResolution.absent();
        }

        if (cfg.isRequireApiKeyForHeader() && !hasApiKeyAuthentication(request)) {
            return TenantResolution.error(tenantApiKeyRequiredError(headerName), headerValue, TenantSource.HEADER);
        }

        return resolveByIdCandidate(headerValue, TenantSource.HEADER);
    }

    private TenantResolution resolveFromSubdomain(HttpServletRequest request) {
        if (!cfg.isAllowSubdomainResolution()) {
            return TenantResolution.absent();
        }

        String host = extractHost(request);
        if (!StringUtils.hasText(host)) {
            return TenantResolution.absent();
        }
        String[] parts = host.split("\\.");
        if (parts.length < 3) {
            return TenantResolution.absent();
        }

        String candidate = parts[0].toLowerCase(Locale.ROOT);
        if (reservedSubdomains.contains(candidate)) {
            return TenantResolution.absent();
        }

        TenantResolution cached = resolveFromCache(candidate);
        if (cached != null) {
            return cached;
        }

        TenantResolution resolution = resolveBySlugCandidate(candidate, TenantSource.SUBDOMAIN);
        cacheSubdomain(candidate, resolution);
        return resolution;
    }

    private TenantResolution resolveFromAdminPath(HttpServletRequest request) {
        if (!cfg.isAllowPathResolution()) {
            return TenantResolution.absent();
        }

        String path = request.getRequestURI();
        if (!StringUtils.hasText(path)) {
            return TenantResolution.absent();
        }

        Matcher matcher = ADMIN_PATH_PATTERN.matcher(path);
        if (!matcher.find()) {
            return TenantResolution.absent();
        }

        if (!hasSuperAdminAuthority()) {
            return TenantResolution.error(tenantSuperAdminRequiredError(), matcher.group(1), TenantSource.ADMIN_PATH);
        }

        return resolveByIdCandidate(matcher.group(1), TenantSource.ADMIN_PATH);
    }

    private TenantResolution resolveByIdCandidate(String raw, TenantSource source) {
        String value = trimToNull(raw);
        if (value == null) {
            return TenantResolution.absent();
        }
        UUID tenantId;
        try {
            tenantId = UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return TenantResolution.invalid(value, source);
        }

        Optional<TenantDirectory.TenantRecord> record = directory.findById(tenantId);
        return evaluateRecord(record, value, source);
    }

    private TenantResolution resolveBySlugCandidate(String raw, TenantSource source) {
        String slug = normalizeSlug(raw);
        if (slug == null) {
            return TenantResolution.invalid(raw, source);
        }

        Optional<TenantDirectory.TenantRecord> record = directory.findBySlug(slug);
        return evaluateRecord(record, slug, source);
    }

    private TenantResolution evaluateRecord(Optional<TenantDirectory.TenantRecord> optional,
                                            String rawValue,
                                            TenantSource source) {
        if (optional.isEmpty()) {
            return TenantResolution.error(tenantNotFoundError(), rawValue, source);
        }

        TenantDirectory.TenantRecord record = optional.get();
        if (record.isInactive()) {
            return TenantResolution.error(tenantInactiveError(), rawValue, source);
        }

        return TenantResolution.present(record.id().toString(), source);
    }

    private void cacheSubdomain(String subdomain, TenantResolution resolution) {
        if (subdomainCacheTtl.isZero() || !resolution.hasTenant()) {
            return;
        }
        Instant expiresAt = Instant.now().plus(subdomainCacheTtl);
        subdomainCache.put(subdomain, new CachedTenant(resolution.tenantId(), expiresAt));
    }

    private TenantResolution resolveFromCache(String subdomain) {
        if (subdomainCacheTtl.isZero()) {
            return null;
        }
        CachedTenant cached = subdomainCache.get(subdomain);
        if (cached == null) {
            return null;
        }
        if (cached.isExpired()) {
            subdomainCache.remove(subdomain, cached);
            return null;
        }
        return TenantResolution.present(cached.tenantId, TenantSource.SUBDOMAIN);
    }

    private boolean hasApiKeyAuthentication(HttpServletRequest request) {
        if (StringUtils.hasText(trimToNull(request.getHeader(HeaderNames.API_KEY)))) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String simpleName = authentication.getClass().getSimpleName();
        if (simpleName.contains("ApiKey")) {
            return true;
        }
        if (authentication.getAuthorities() != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String value = authority.getAuthority();
                if (value != null && ("ROLE_API_CLIENT".equals(value) || "ROLE_SERVICE".equals(value))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSuperAdminAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value == null) {
                continue;
            }
            if ("SUPER_ADMIN".equals(value) || "ROLE_SUPER_ADMIN".equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String extractHost(HttpServletRequest request) {
        String host = trimToNull(request.getHeader("Host"));
        if (!StringUtils.hasText(host)) {
            host = trimToNull(request.getServerName());
        }
        if (host == null) {
            return null;
        }
        int colon = host.indexOf(':');
        return colon >= 0 ? host.substring(0, colon) : host;
    }

    private static String normalizeSlug(String slug) {
        String normalized = trimToNull(slug);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!SLUG_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private static String firstClaim(Map<String, Object> claims, String[] names) {
        if (claims == null || names == null) {
            return null;
        }
        for (String name : names) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Object value = claims.get(name);
            String asString = trimToNull(asString(value));
            if (asString != null) {
                return asString;
            }
        }
        return null;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        return value.toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String safePath(HttpServletRequest request) {
        try {
            return request.getRequestURI();
        } catch (Exception ex) {
            return "<unknown>";
        }
    }

    private static TenantError tenantMissingError() {
        return TenantError.badRequest("TENANT_REQUIRED", "Tenant identification required");
    }

    private static TenantError tenantNotFoundError() {
        return TenantError.notFound("TENANT_NOT_FOUND", "Tenant not available");
    }

    private static TenantError tenantInactiveError() {
        return TenantError.forbidden("TENANT_INACTIVE", "Service unavailable");
    }

    private static TenantError tenantApiKeyRequiredError(String headerName) {
        String message = "API key authentication required to use " + headerName;
        return TenantError.unauthorized("TENANT_API_KEY_REQUIRED", message);
    }

    private static TenantError tenantSuperAdminRequiredError() {
        return TenantError.forbidden("TENANT_SUPER_ADMIN_REQUIRED", "Super admin privileges required");
    }

    private static void logResolvedTenant(TenantResolution resolution, HttpServletRequest request) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("Resolved tenant {} via {} for {} {}", resolution.tenantId(), resolution.source(),
                request.getMethod(), safePath(request));
    }

    private static void logResolutionError(TenantResolution resolution, HttpServletRequest request) {
        TenantError error = resolution.error();
        String message = error != null ? error.code() : "TENANT_ERROR";
        log.warn("Tenant resolution error (source={}, status={}): {} on {} {}", resolution.source(),
                error != null ? error.httpStatus() : "n/a", message, request.getMethod(), safePath(request));
    }

    private record CachedTenant(String tenantId, Instant expiresAt) {
        private boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }
}

