package com.ejada.sec.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateUserRequest {
  @Email @Size(max = 255) private String email;
  @JsonAlias({"phone_number", "phoneNumber"})
  @Size(max = 32) private String phoneNumber;
  private Boolean enabled;
  private Boolean locked;
  private List<@NotBlank String> roles;
}
