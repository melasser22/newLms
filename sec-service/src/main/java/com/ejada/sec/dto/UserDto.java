package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDto {
  private Long id;
  @NotNull private UUID tenantId;
  @NotNull private UUID internalTenantId;
  @NotBlank @Size(max = 120) private String username;
  @Email @NotBlank @Size(max = 255) private String email;
  private boolean enabled;
  private boolean locked;
  private boolean firstLoginCompleted;
  private Instant passwordChangedAt;
  private Instant passwordExpiresAt;
  private List<String> roles;
  private List<String> privileges;
}
