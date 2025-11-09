package com.ejada.subscription.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "subscription.security.jwt.secret=abcdefghijklmnopqrstuvwxyz0123456789abcd",
      "subscription.security.jwt.expiration=PT30M",
      "subscription.security.users[0].login-name=tester",
      "subscription.security.users[0].password=b03ddf3ca2e714a6548e7495e2a03f5e824eaac9837cd7f159c67b90fb4b7342"
    })
@AutoConfigureMockMvc
class SubscriptionAuthIntegrationTest {

  private static final String PASSWORD =
      "b03ddf3ca2e714a6548e7495e2a03f5e824eaac9837cd7f159c67b90fb4b7342";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldIssueTokenForValidCredentials() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/subscription/get-token")
                    .header("rqUID", "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{" + "\"loginName\":\"tester\"," + "\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    assertThat(root.path("success").asBoolean()).isTrue();
    String token = root.path("returnedObject").path("token").asText();
    assertThat(token).isNotBlank();

    var claimsJws =
        Jwts.parser()
            .verifyWith(
                Keys.hmacShaKeyFor("abcdefghijklmnopqrstuvwxyz0123456789abcd".getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token);
    assertThat(claimsJws.getPayload().getSubject()).isEqualTo("tester");
    assertThat(claimsJws.getPayload().get("scope", String.class)).isEqualTo("subscription");
  }

  @Test
  void shouldReturnUnauthorizedForInvalidPassword() throws Exception {
    mockMvc
        .perform(
            post("/subscription/get-token")
                .header("rqUID", "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"loginName\":\"tester\"," + "\"password\":\"" + PASSWORD.replace('b', 'c') + "\"}"))
        .andExpect(status().isUnauthorized());
  }
}
