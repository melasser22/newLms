package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
  @NotNull private UUID tenantId;
  @NotBlank @Size(max=120) private String username;
  @Email @NotBlank @Size(max=255) private String email;
  @NotBlank @Size(min=8, max=120) private String password;
  @Singular private List<@NotBlank String> roles;
}
