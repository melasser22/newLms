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
public class FederatedLinkRequest extends BaseRequest {
  @NotNull private Long userId;
  @NotBlank private String provider;
  @NotBlank private String providerUserId;
}
