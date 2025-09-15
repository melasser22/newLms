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
public class RevokePrivilegesFromRoleRequest extends BaseRequest {
  @NotBlank private String roleCode;
  private List<@NotBlank String> privilegeCodes;
}
