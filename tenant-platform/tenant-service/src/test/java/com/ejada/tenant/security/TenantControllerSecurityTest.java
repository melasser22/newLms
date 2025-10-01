package com.ejada.tenant.security;

import com.ejada.starter_security.SecurityAutoConfiguration;
import com.ejada.tenant.controller.TenantController;
import com.ejada.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantController.class)
@Import({SecurityAutoConfiguration.class, TenantAccessPolicy.class})
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=" + TenantControllerSecurityTest.SECRET,
    "shared.security.jwt.secret=" + TenantControllerSecurityTest.SECRET,
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health",
    "server.servlet.context-path=/tenant"
})
class TenantControllerSecurityTest {

    static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void tenantEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/tenant/api/v1/tenants"))
            .andExpect(status().isUnauthorized());
    }
}
