package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDto {
  private Long id;
  @NotNull private UUID tenantId;
  @NotNull private UUID internalTenantId;
  @NotBlank @Size(max = 120) private String username;
  @Email @NotBlank @Size(max = 255) private String email;
  private boolean enabled;
  private boolean locked;
  private List<String> roles;
  private List<String> privileges;
}
