package com.ejada.setup.security;

import com.ejada.setup.controller.CountryController;
import com.ejada.setup.service.CountryService;
import com.ejada.starter_security.SecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CountryController.class)
@Import(SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=test-secret",
    "shared.security.jwt.secret=test-secret",
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health",
    "server.servlet.context-path=/core"
})
class CountryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CountryService countryService;

    @Test
    void protectedEndpointsReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/core/setup/countries"))
            .andExpect(status().isUnauthorized());
    }
}
