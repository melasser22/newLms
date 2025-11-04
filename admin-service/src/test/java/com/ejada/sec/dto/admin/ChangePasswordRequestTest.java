package com.ejada.sec.dto.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ChangePasswordRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSnakeCasePayload() throws Exception {
        String json = """
            {
              "current_password": "Admin@123!",
              "new_password": "DifferentPass123!",
              "confirm_password": "DifferentPass123!"
            }
            """;

        ChangePasswordRequest request = objectMapper.readValue(json, ChangePasswordRequest.class);

        assertThat(request.getCurrentPassword()).isEqualTo("Admin@123!");
        assertThat(request.getNewPassword()).isEqualTo("DifferentPass123!");
        assertThat(request.getConfirmPassword()).isEqualTo("DifferentPass123!");
    }
}
