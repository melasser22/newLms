package com.ejada.admin.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuperadminLoginRequest {
    @NotBlank(message = "Username or email is required")
    private String identifier; // username or email
    
    @NotBlank(message = "Password is required")
    private String password;
}
