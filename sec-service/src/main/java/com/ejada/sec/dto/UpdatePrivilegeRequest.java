package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdatePrivilegeRequest {
  @Size(max=200) private String name;
  @Size(max=500) private String description;
}
