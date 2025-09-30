package com.ejada.sec.security;

import com.ejada.sec.controller.UserController;
import com.ejada.sec.service.UserService;
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

@WebMvcTest(controllers = UserController.class)
@Import(SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "shared.security.mode=hs256",
    "shared.security.hs256.secret=test-secret",
    "shared.security.jwt.secret=test-secret",
    "shared.security.resource-server.enabled=true",
    "shared.security.resource-server.disable-csrf=true",
    "shared.security.resource-server.permit-all[0]=/actuator/health",
    "server.servlet.context-path=/sec"
})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void userEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/sec/api/users"))
            .andExpect(status().isUnauthorized());
    }
}
