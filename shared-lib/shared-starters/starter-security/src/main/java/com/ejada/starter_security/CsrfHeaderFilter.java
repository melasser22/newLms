package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Exposes the CSRF token value via {@code X-CSRF-Token} header so that
 * JavaScript clients can read it on the first request and echo it back in
 * subsequent state changing requests.
 */
class CsrfHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            response.setHeader(HeaderNames.CSRF_TOKEN, token.getToken());
        }
        filterChain.doFilter(request, response);
    }
}
