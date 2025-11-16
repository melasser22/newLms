package com.ejada.sec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteFirstLoginRequest {
  @NotBlank private String identifier;
  @NotBlank private String currentPassword;
  @NotBlank
  @Size(min = 8, max = 120)
  private String newPassword;
  @NotBlank private String confirmPassword;
}
