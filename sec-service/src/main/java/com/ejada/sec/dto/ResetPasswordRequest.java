package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResetPasswordRequest {
  @NotNull private UUID tenantId;
  @NotBlank private String resetToken;
  @NotBlank @Size(min=8, max=120) private String newPassword;
}
