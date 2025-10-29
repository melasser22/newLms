package com.ejada.tenant.security;

import com.ejada.starter_security.SecurityAutoConfiguration;
import com.ejada.tenant.controller.TenantController;
import com.ejada.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = TenantController.class)
@Import({SecurityAutoConfiguration.class, TenantAccessPolicy.class})
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=" + TenantControllerSecurityTest.SECRET,
    "shared.security.jwt.secret=" + TenantControllerSecurityTest.SECRET,
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health"
})
class TenantControllerSecurityTest {

    static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantService tenantService;

    @MockBean(name = "tenantAccessPolicy")
    private TenantAccessPolicy tenantAccessPolicy;

    @Test
    void tenantEndpointsRequireAuthentication() {
        when(tenantAccessPolicy.isAllowed(any())).thenReturn(true);

        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/tenants")
                        .header("X-Tenant-Id", "tenant-123")))
                .hasRootCauseInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
