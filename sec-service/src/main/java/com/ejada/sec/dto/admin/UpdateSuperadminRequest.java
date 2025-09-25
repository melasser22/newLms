package com.ejada.sec.dto.admin;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateSuperadminRequest {
    
    @Email(message = "Email must be valid")
    private String email;
    
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    private String phoneNumber;
    
    private Boolean enabled;
    private Boolean locked;
}