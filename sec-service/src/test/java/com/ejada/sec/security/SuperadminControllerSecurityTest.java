package com.ejada.sec.security;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.config.MethodSecurityConfig;
import com.ejada.sec.controller.SuperadminController;
import com.ejada.sec.service.SuperadminService;
import com.ejada.starter_security.SecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SuperadminController.class)
@Import({SecurityAutoConfiguration.class, MethodSecurityConfig.class})
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
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SuperadminService superadminService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void superadminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/admins"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EJADA_OFFICER")
    void superadminEndpointsAllowEjadaOfficer() throws Exception {
        when(superadminService.listSuperadmins(any(Pageable.class)))
            .thenReturn(BaseResponse.success("ok", Page.empty()));

        mockMvc.perform(get("/api/v1/auth/admins"))
            .andExpect(status().isOk());
    }
}
