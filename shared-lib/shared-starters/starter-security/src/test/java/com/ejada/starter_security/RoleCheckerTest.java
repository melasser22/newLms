package com.ejada.starter_security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class RoleCheckerTest {

    private RoleChecker newRoleChecker() {
        SharedSecurityProps props = new SharedSecurityProps();
        props.setEnableRoleCheck(true);
        props.setRolePrefix("ROLE_");
        return new RoleChecker(props);
    }

    @Test
    void hasRoleAcceptsAuthoritiesWithConfiguredPrefix() {
        RoleChecker checker = newRoleChecker();
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_EJADA_OFFICER");
        TestingAuthenticationToken authentication =
            new TestingAuthenticationToken("principal", "credentials", List.of(authority));
        authentication.setAuthenticated(true);

        assertThat(checker.hasRole(authentication, Role.EJADA_OFFICER)).isTrue();
    }

    @Test
    void hasRoleAcceptsAuthoritiesWithoutPrefix() {
        RoleChecker checker = newRoleChecker();
        GrantedAuthority authority = new SimpleGrantedAuthority("EJADA_OFFICER");
        TestingAuthenticationToken authentication =
            new TestingAuthenticationToken("principal", "credentials", List.of(authority));
        authentication.setAuthenticated(true);

        assertThat(checker.hasRole(authentication, Role.EJADA_OFFICER)).isTrue();
    }

    @Test
    void hasRoleReturnsFalseWhenAuthenticationHasNoAuthorities() {
        RoleChecker checker = newRoleChecker();
        TestingAuthenticationToken authentication =
            new TestingAuthenticationToken("principal", "credentials");
        authentication.setAuthenticated(true);

        assertThat(checker.hasRole(authentication, Role.EJADA_OFFICER)).isFalse();
    }
}
