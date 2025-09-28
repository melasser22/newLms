package com.ejada.starter_security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class SecurityAutoConfigurationCorsTest {

    private final SecurityAutoConfiguration configuration = new SecurityAutoConfiguration();

    @Test
    void allowsCredentialsWhenCsrfEnabled() {
        SharedSecurityProps props = new SharedSecurityProps();
        props.getResourceServer().setDisableCsrf(false);

        CorsConfiguration cors = configuration.corsConfigurationSource(props)
            .getCorsConfiguration(new MockHttpServletRequest());

        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()));
    }

    @Test
    void disallowsCredentialsWhenCsrfDisabled() {
        SharedSecurityProps props = new SharedSecurityProps();
        props.getResourceServer().setDisableCsrf(true);

        CorsConfiguration cors = configuration.corsConfigurationSource(props)
            .getCorsConfiguration(new MockHttpServletRequest());

        assertFalse(Boolean.TRUE.equals(cors.getAllowCredentials()));
    }
}
