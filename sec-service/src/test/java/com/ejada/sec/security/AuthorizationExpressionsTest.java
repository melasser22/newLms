package com.ejada.sec.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class AuthorizationExpressionsTest {

    private AuthorizationExpressions expressions;

    @BeforeEach
    void setUp() {
        expressions = new AuthorizationExpressions();
    }

    @Test
    void returnsTrueWhenAuthenticationHasEjadaOfficerRole() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_EJADA_OFFICER");
        authentication.setAuthenticated(true);

        assertTrue(expressions.isEjadaOfficer(authentication));
    }

    @Test
    void returnsFalseWhenAuthenticationMissingRole() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password",
                "ROLE_TENANT_ADMIN");
        authentication.setAuthenticated(true);

        assertFalse(expressions.isEjadaOfficer(authentication));
    }

    @Test
    void returnsFalseWhenAuthenticationNullOrUnauthenticated() {
        assertFalse(expressions.isEjadaOfficer(null));

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                "user",
                "password");
        authentication.setAuthenticated(false);

        assertFalse(expressions.isEjadaOfficer(authentication));
    }
}
