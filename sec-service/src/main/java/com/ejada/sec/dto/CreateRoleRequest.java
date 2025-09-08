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
public class CreateRoleRequest extends BaseRequest {
  @NotBlank @Size(max = 64) private String code;
  @NotBlank @Size(max = 128) private String name;
}
