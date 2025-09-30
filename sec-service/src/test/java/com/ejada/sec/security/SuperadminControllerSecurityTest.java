package com.ejada.sec.security;

import com.ejada.sec.controller.SuperadminController;
import com.ejada.sec.service.SuperadminService;
import com.ejada.starter_security.SecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SuperadminController.class)
@Import(SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=0123456789ABCDEF0123456789ABCDEF-SECURE-0123456789ABCDEF",
    "shared.security.jwt.secret=0123456789ABCDEF0123456789ABCDEF-SECURE-0123456789ABCDEF",
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health"
})
class SuperadminControllerSecurityTest {

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuperadminService superadminService;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void superadminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/superadmin/admins"))
            .andExpect(status().isUnauthorized());
    }
}
