package com.ejada.sec.dto;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSummary {
  private Long id;
  private UUID tenantId;
  private String username;
  private String email;
  private boolean enabled;
  private boolean locked;
}
