package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PrivilegeDto {
  private Long id;
  @NotNull private UUID tenantId;
    @NotBlank @Size(max = 100) private String code;
    @NotBlank @Size(max = 100) private String resource;
    @NotBlank @Size(max = 50)  private String action;
    @NotBlank @Size(max = 200) private String name;
    @Size(max = 500) private String description;
}
