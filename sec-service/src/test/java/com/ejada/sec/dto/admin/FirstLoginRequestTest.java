package com.ejada.sec.dto.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class FirstLoginRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSnakeCasePayload() throws Exception {
        String json = """
            {
              "current_password": "Admin@123!",
              "new_password": "StrongerPass123!",
              "confirm_password": "StrongerPass123!",
              "first_name": "Jane",
              "last_name": "Doe",
              "phone_number": "+15551234567"
            }
            """;

        FirstLoginRequest request = objectMapper.readValue(json, FirstLoginRequest.class);

        assertThat(request.getCurrentPassword()).isEqualTo("Admin@123!");
        assertThat(request.getNewPassword()).isEqualTo("StrongerPass123!");
        assertThat(request.getConfirmPassword()).isEqualTo("StrongerPass123!");
        assertThat(request.getFirstName()).isEqualTo("Jane");
        assertThat(request.getLastName()).isEqualTo("Doe");
        assertThat(request.getPhoneNumber()).isEqualTo("+15551234567");
    }

    @Test
    void deserializesCamelCasePayload() throws Exception {
        String json = """
            {
              "currentPassword": "Admin@123!",
              "newPassword": "StrongerPass123!",
              "confirmPassword": "StrongerPass123!",
              "firstName": "Jane",
              "lastName": "Doe",
              "phoneNumber": "+15551234567"
            }
            """;

        FirstLoginRequest request = objectMapper.readValue(json, FirstLoginRequest.class);

        assertThat(request.getCurrentPassword()).isEqualTo("Admin@123!");
        assertThat(request.getNewPassword()).isEqualTo("StrongerPass123!");
        assertThat(request.getConfirmPassword()).isEqualTo("StrongerPass123!");
        assertThat(request.getFirstName()).isEqualTo("Jane");
        assertThat(request.getLastName()).isEqualTo("Doe");
        assertThat(request.getPhoneNumber()).isEqualTo("+15551234567");
    }
}
