package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
  @NotBlank private String accessToken;
  @NotBlank private String refreshToken;
  @Builder.Default private String tokenType = "Bearer";
  private long expiresInSeconds;
}
