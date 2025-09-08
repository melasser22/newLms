package com.ejada.sec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FederatedLinkRequest {
  @NotNull private UUID tenantId;
  @NotNull private Long userId;
  @NotBlank private String provider;
  @NotBlank private String providerUserId;
}
