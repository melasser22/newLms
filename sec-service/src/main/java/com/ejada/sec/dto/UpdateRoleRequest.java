package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateRoleRequest {
  @Size(max=128) private String name;
}
