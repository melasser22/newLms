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
public class CreatePrivilegeRequest extends BaseRequest {
  @NotBlank @Size(max = 100) private String code;
  @NotBlank @Size(max = 100) private String resource;
  @NotBlank @Size(max = 50) private String action;
  @NotBlank @Size(max = 200) private String name;
  @Size(max = 500) private String description;
}
