package com.ejada.starter_security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.starter_security.Role;
import com.ejada.starter_security.RoleChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

class AuthorizationExpressionsTest {

    @Mock
    private RoleChecker roleChecker;

    @Mock
    private Authentication authentication;

    private AuthorizationExpressions expressions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        expressions = new AuthorizationExpressions(roleChecker);
    }

    @Test
    void hasAnyRoleDelegatesToRoleChecker() {
        when(roleChecker.hasRole(authentication, Role.EJADA_OFFICER)).thenReturn(true);

        boolean result = expressions.hasAnyRole(authentication, Role.EJADA_OFFICER);

        assertThat(result).isTrue();
        verify(roleChecker).hasRole(authentication, Role.EJADA_OFFICER);
    }

    @Test
    void isEjadaOfficerUsesHelper() {
        expressions.isEjadaOfficer(authentication);

        verify(roleChecker).hasRole(authentication, Role.EJADA_OFFICER);
    }

    @Test
    void isPlatformStaffCoversAllPlatformRoles() {
        expressions.isPlatformStaff(authentication);

        verify(roleChecker).hasRole(authentication,
                Role.EJADA_OFFICER,
                Role.TENANT_ADMIN,
                Role.TENANT_OFFICER);
    }
}
