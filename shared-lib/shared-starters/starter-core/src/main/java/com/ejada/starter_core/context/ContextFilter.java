package com.ejada.starter_core.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Single servlet filter responsible for wiring correlation/tenant/user context handling.
 *
 * <p>The filter delegates to pluggable {@link RequestContextContributor RequestContextContributors} which
 * can extend or replace default behaviour.  Downstream applications may register additional
 * contributors to populate custom MDC keys or thread-local state.</p>
 */
public class ContextFilter extends OncePerRequestFilter {

    /** Default URI prefixes that skip context propagation altogether. */
    public static final Set<String> DEFAULT_SKIP_PREFIXES = Set.of(
        "/actuator", "/health", "/error", "/v3/api-docs", "/swagger", "/swagger-ui", "/docs"
    );


    private static final Logger logger = LoggerFactory.getLogger(ContextFilter.class);

    private final List<RequestContextContributor> contributors;
    private final Set<String> skipPrefixes;
    private final List<String> skipPatterns;
    private final PathMatcherDelegate matcher = new PathMatcherDelegate();

    public ContextFilter(List<RequestContextContributor> contributors,
                         Set<String> skipPrefixes,
                         List<String> skipPatterns) {
        List<RequestContextContributor> ordered = new ArrayList<>(contributors == null ? List.of() : contributors);
        AnnotationAwareOrderComparator.sort(ordered);
        this.contributors = Collections.unmodifiableList(ordered);
        Set<String> combinedPrefixes = new LinkedHashSet<>(DEFAULT_SKIP_PREFIXES);
        if (skipPrefixes != null) {
            combinedPrefixes.addAll(skipPrefixes);
        }
        this.skipPrefixes = Collections.unmodifiableSet(combinedPrefixes);
        this.skipPatterns = (skipPatterns == null) ? List.of() : List.copyOf(skipPatterns);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        final String path = request.getRequestURI();
        for (String prefix : skipPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        for (String pattern : skipPatterns) {
            if (matcher.matches(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        List<RequestContextContributor.ContextScope> scopes = new ArrayList<>(contributors.size());
        try {
            for (RequestContextContributor contributor : contributors) {
                RequestContextContributor.ContextScope scope = contributor.contribute(request, response);
                if (scope != null) {
                    scopes.add(scope);
                }
                if (response.isCommitted()) {
                    break;
                }
            }
            if (!response.isCommitted()) {
                filterChain.doFilter(request, response);
            }
        } finally {
            for (int i = scopes.size() - 1; i >= 0; i--) {
                RequestContextContributor.ContextScope scope = scopes.get(i);
                try {
                    scope.close();
                } catch (Exception ex) {
                    logger.warn("Failed to cleanup request context scope from {}", scope.getClass().getName(), ex);
                }
            }

        }
    }

    /**
     * Minimal Ant-style path matcher delegate so we do not pull in the entire Spring utility
     * namespace.  We only need a matcher when skip patterns are configured.
     */
    static final class PathMatcherDelegate {
        private final org.springframework.util.AntPathMatcher delegate = new org.springframework.util.AntPathMatcher();

        boolean matches(String pattern, String path) {
            if (pattern == null || pattern.isBlank()) {
                return false;
            }
            return delegate.match(pattern, path);
        }

    }
}
