package com.ejada.sec.security;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.controller.RoleController;
import com.ejada.sec.service.RoleService;
import com.ejada.starter_security.SecurityAutoConfiguration;
import com.ejada.testsupport.security.JwtTestTokens;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RoleController.class)
@Import(SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=0123456789ABCDEF0123456789ABCDEF-SECURE-0123456789ABCDEF",
    "shared.security.jwt.secret=0123456789ABCDEF0123456789ABCDEF-SECURE-0123456789ABCDEF",

    "shared.security.issuer=" + RoleControllerSecurityTest.ISSUER,
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health"
})
class RoleControllerSecurityTest {

    private static final String SECRET =
            "0123456789ABCDEF0123456789ABCDEF-SECURE-0123456789ABCDEF";
    static final String ISSUER = "test-issuer";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @BeforeEach
    void setup() {
        when(roleService.listByTenant()).thenReturn(BaseResponse.success("Roles", List.of()));
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenWithRequiredRoleIsAccepted() throws Exception {
        String token = JwtTestTokens.hs256(SECRET)
                .issuer(ISSUER)
                .roles(List.of("TENANT_ADMIN"))
                .tenant("tenant-1")
                .build();

        mockMvc.perform(get("/api/v1/roles")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS-200"));
    }

    @Test
    void tokenWithoutRequiredRoleIsForbidden() throws Exception {
        String token = JwtTestTokens.hs256(SECRET)
                .issuer(ISSUER)
                .roles(List.of("TENANT_VIEWER"))
                .tenant("tenant-1")
                .build();

        mockMvc.perform(get("/api/v1/roles")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_FORBIDDEN));
    }

    @Test
    void expiredTokenIsRejectedAsUnauthorized() throws Exception {
        String token = JwtTestTokens.hs256(SECRET)
                .issuer(ISSUER)
                .roles(List.of("TENANT_ADMIN"))
                .tenant("tenant-1")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        mockMvc.perform(get("/api/v1/roles")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_UNAUTHORIZED));
    }
}
