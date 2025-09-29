package com.ejada.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class TenantAccessPolicyTest {

    @Test
    void allowsConfiguredRole() {
        TenantAccessPolicy policy = new TenantAccessPolicy("ROLE_ADMIN, EJADA_OFFICER");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pwd", "EJADA_OFFICER");
        auth.setAuthenticated(true);

        assertThat(policy.isAllowed(auth)).isTrue();
    }

    @Test
    void deniesWhenNotConfigured() {
        TenantAccessPolicy policy = new TenantAccessPolicy("ROLE_ADMIN");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pwd", Collections.emptyList());
        auth.setAuthenticated(true);

        assertThat(policy.isAllowed(auth)).isFalse();
    }

    @Test
    void deniesForAnonymous() {
        TenantAccessPolicy policy = new TenantAccessPolicy("ROLE_ADMIN");

        assertThat(policy.isAllowed(null)).isFalse();
    }
}
