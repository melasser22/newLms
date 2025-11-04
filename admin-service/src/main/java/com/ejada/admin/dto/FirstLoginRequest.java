package com.ejada.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FirstLoginRequest {
    
    @NotBlank(message = "Current password is required")
    @JsonAlias({"current_password", "currentPassword"})
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @JsonAlias({"new_password", "newPassword"})
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @JsonAlias({"confirm_password", "confirmPassword"})
    private String confirmPassword;

    // Optional profile updates during first login
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @JsonAlias({"first_name", "firstName"})
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @JsonAlias({"last_name", "lastName"})
    private String lastName;

    @Pattern(
        regexp = "^\\+?[0-9]{10,15}$",
        message = "Phone number must be valid"
    )
    @JsonAlias({"phone_number", "phoneNumber"})
    private String phoneNumber;
}
