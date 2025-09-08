package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenRequest {
  @NotNull private UUID tenantId;
  @NotBlank private String refreshToken;
}
