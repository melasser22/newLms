package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthRequest {
  @NotBlank private String identifier; // username or email
  @NotBlank private String password;
}
