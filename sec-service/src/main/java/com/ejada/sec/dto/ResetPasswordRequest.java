package com.ejada.sec.dto;

import com.ejada.common.dto.BaseRequest;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class ResetPasswordRequest extends BaseRequest {
  @NotBlank private String resetToken;
  @NotBlank @Size(min = 8, max = 120) private String newPassword;
}
