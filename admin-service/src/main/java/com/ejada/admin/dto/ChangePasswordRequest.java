package com.ejada.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    @JsonAlias("current_password")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @JsonAlias("new_password")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @JsonAlias("confirm_password")
    private String confirmPassword;
}
