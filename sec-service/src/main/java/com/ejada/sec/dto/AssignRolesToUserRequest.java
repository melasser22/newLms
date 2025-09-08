package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignRolesToUserRequest {
  @NotNull private UUID tenantId;
  @NotNull private Long userId;
  private List<@NotBlank String> roleCodes;
}
