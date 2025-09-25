package com.ejada.sec.dto.admin;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirstLoginRequest {
    
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    // Optional profile updates during first login
    private String firstName;
    private String lastName;
    private String phoneNumber;
}
