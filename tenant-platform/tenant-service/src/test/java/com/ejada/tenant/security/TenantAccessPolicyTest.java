package com.ejada.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.starter_security.SharedSecurityProps;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class TenantAccessPolicyTest {

    private final SharedSecurityProps props = new SharedSecurityProps();

    private TenantAccessPolicy createPolicy(final String roles) {
        return new TenantAccessPolicy(roles, props);
    }

    @Test
    void allowsConfiguredRole() {
        TenantAccessPolicy policy = createPolicy("ROLE_ADMIN, EJADA_OFFICER");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pwd", "ROLE_EJADA_OFFICER");
        auth.setAuthenticated(true);

        assertThat(policy.getAllowedRoles()).contains("ROLE_EJADA_OFFICER");
        assertThat(policy.isAllowed(auth)).isTrue();
    }

    @Test
    void deniesWhenNotConfigured() {
        TenantAccessPolicy policy = createPolicy("ROLE_ADMIN");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pwd", Collections.emptyList());
        auth.setAuthenticated(true);

        assertThat(policy.isAllowed(auth)).isFalse();
    }

    @Test
    void deniesForAnonymous() {
        TenantAccessPolicy policy = createPolicy("ROLE_ADMIN");

        assertThat(policy.isAllowed(null)).isFalse();
    }

    @Test
    void trimsAndIgnoresBlankConfiguration() {
        TenantAccessPolicy policy = createPolicy("   ");
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pwd", "ROLE_EJADA_OFFICER");
        auth.setAuthenticated(true);

        assertThat(policy.getAllowedRoles()).isEmpty();
        assertThat(policy.isAllowed(auth)).isFalse();
    }

    @Test
    void nullConfigurationDisablesAccess() {
        TenantAccessPolicy policy = createPolicy(null);
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pwd", "ROLE_EJADA_OFFICER");
        auth.setAuthenticated(true);

        assertThat(policy.getAllowedRoles()).isEmpty();
        assertThat(policy.isAllowed(auth)).isFalse();
    }
}
