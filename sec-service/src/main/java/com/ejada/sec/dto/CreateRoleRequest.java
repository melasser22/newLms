package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateRoleRequest {
  @NotNull private UUID tenantId;
  @NotBlank @Size(max=64) private String code;
  @NotBlank @Size(max=128) private String name;
}
