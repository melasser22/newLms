package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateUserRequest {
  @Email @Size(max = 255) private String email;
  private Boolean enabled;
  private Boolean locked;
  private List<@NotBlank String> roles;
}
