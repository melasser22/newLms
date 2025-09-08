package com.ejada.sec.dto;

import com.ejada.common.dto.BaseRequest;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CreateUserRequest extends BaseRequest {
  @NotBlank @Size(max = 120) private String username;
  @Email @NotBlank @Size(max = 255) private String email;
  @NotBlank @Size(min = 8, max = 120) private String password;
  private List<@NotBlank String> roles;
}
