package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthRequest {
  @NotNull private UUID tenantId;
  @NotBlank private String identifier; // username or email
  @NotBlank private String password;
}
