package com.ejada.sec.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.common.dto.BaseResponse;
import com.ejada.sec.dto.AuthRequest;
import com.ejada.sec.dto.AuthResponse;
import com.ejada.sec.service.AuthService;
import com.ejada.sec.service.PasswordResetService;
import com.ejada.sec.service.SuperadminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean SuperadminService superadminService;
    @MockitoBean PasswordResetService passwordResetService;
    @MockitoBean JpaMetamodelMappingContext jpaMappingContext;

    @BeforeEach
    void resetMocks() {
        reset(authService, superadminService, passwordResetService);
    }

    @Test
    void login_success_returnsOk() throws Exception {
        AuthResponse tokens = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresInSeconds(3600)
                .build();
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(BaseResponse.success("Login successful", tokens));

        AuthRequest request = AuthRequest.builder()
                .tenantId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .identifier("user@example.com")
                .password("password")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS-200"));
    }

    @Test
    void login_failure_returnsUnauthorized() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(BaseResponse.error("ERR-AUTH-INVALID", "Invalid credentials"));

        AuthRequest request = AuthRequest.builder()
                .tenantId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .identifier("user@example.com")
                .password("wrong")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ERR-AUTH-INVALID"));
    }
}
