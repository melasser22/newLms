package com.ejada.setup.security;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.controller.CountryController;
import com.ejada.setup.service.CountryService;
import com.ejada.starter_security.SecurityAutoConfiguration;
import com.ejada.testsupport.security.JwtTestTokens;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CountryController.class)
@Import(SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=test-secret",
    "shared.security.jwt.secret=test-secret",
    "shared.security.issuer=" + CountryControllerSecurityTest.ISSUER,
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health",
    "server.servlet.context-path=/core"
})
class CountryControllerSecurityTest {

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";
    static final String ISSUER = "test-issuer";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CountryService countryService;

    @BeforeEach
    void setup() {
        when(countryService.list(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(BaseResponse.success("Countries", List.of()));
    }

    @Test
    void protectedEndpointsReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/core/setup/countries"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenAllowsAccess() throws Exception {
        String token = JwtTestTokens.hs256(SECRET)
                .issuer(ISSUER)
                .roles(List.of("TENANT_ADMIN"))
                .tenant("tenant-1")
                .build();

        mockMvc.perform(get("/core/setup/countries")
                        .header(AUTHORIZATION, "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS-200"));
    }

    @Test
    void tokenMissingRequiredRoleIsForbidden() throws Exception {
        String token = JwtTestTokens.hs256(SECRET)
                .issuer(ISSUER)
                .roles(List.of("TENANT_VIEWER"))
                .tenant("tenant-1")
                .build();

        mockMvc.perform(get("/core/setup/countries")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_FORBIDDEN));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = JwtTestTokens.hs256(SECRET)
                .issuer(ISSUER)
                .roles(List.of("TENANT_ADMIN"))
                .tenant("tenant-1")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        mockMvc.perform(get("/core/setup/countries")
                        .header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_UNAUTHORIZED));
    }
}
