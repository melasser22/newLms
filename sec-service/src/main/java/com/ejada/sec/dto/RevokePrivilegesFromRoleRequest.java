package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevokePrivilegesFromRoleRequest {
  @NotNull private UUID tenantId;
  @NotBlank private String roleCode;
  private List<@NotBlank String> privilegeCodes;
}
