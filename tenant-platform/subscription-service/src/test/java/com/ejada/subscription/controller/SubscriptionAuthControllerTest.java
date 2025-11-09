package com.ejada.subscription.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.subscription.exception.InvalidCredentialsException;
import com.ejada.subscription.handler.RestExceptionHandler;
import com.ejada.subscription.service.SubscriptionAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SubscriptionAuthControllerTest {

  private static final String URL = "/subscription/get-token";
  private static final String VALID_RQUID = "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd";
  private static final String PASSWORD =
      "b03ddf3ca2e714a6548e7495e2a03f5e824eaac9837cd7f159c67b90fb4b7342";

  @Mock private SubscriptionAuthService authService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    SubscriptionAuthController controller = new SubscriptionAuthController(authService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new RestExceptionHandler())
            .build();
  }

  @Test
  void shouldReturnTokenWhenRequestValid() throws Exception {
    when(authService.authenticate("xyz", PASSWORD)).thenReturn("jwt-token");

    mockMvc
        .perform(
            post(URL)
                .header("rqUID", VALID_RQUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"loginName\":\"xyz\"," + "\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("I000000"))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.returnedObject.token").value("jwt-token"));
  }

  @Test
  void shouldReturnBadRequestWhenRqUidInvalid() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .header("rqUID", "invalid-rquid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"loginName\":\"xyz\"," + "\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.statusDtls[0]").value("Invalid rqUID format"));
  }

  @Test
  void shouldReturnBadRequestWhenBodyInvalid() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .header("rqUID", VALID_RQUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"loginName\":\"\"," + "\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.statusDtls[0]").exists());
  }

  @Test
  void shouldReturnUnauthorizedWhenCredentialsInvalid() throws Exception {
    when(authService.authenticate(anyString(), anyString())).thenThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post(URL)
                .header("rqUID", VALID_RQUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"loginName\":\"xyz\"," + "\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.statusDtls[0]").value("Invalid credentials"));
  }
}
