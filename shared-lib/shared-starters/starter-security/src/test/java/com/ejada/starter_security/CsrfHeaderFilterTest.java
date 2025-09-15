package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.junit.jupiter.api.Assertions.*;

class CsrfHeaderFilterTest {

    @Test
    void copiesTokenToHeader() throws ServletException, IOException {
        CsrfToken token = new DefaultCsrfToken("X-CSRF-Token", "_csrf", "abc123");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(CsrfToken.class.getName(), token);
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {};
        new CsrfHeaderFilter().doFilter(req, res, chain);

        assertEquals("abc123", res.getHeader(HeaderNames.CSRF_TOKEN));
    }

    @Test
    void skipsWhenNoToken() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> {};
        new CsrfHeaderFilter().doFilter(req, res, chain);

        assertNull(res.getHeader(HeaderNames.CSRF_TOKEN));
    }
}
