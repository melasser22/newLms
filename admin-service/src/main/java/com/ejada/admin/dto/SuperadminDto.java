package com.ejada.admin.dto;


import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuperadminDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Boolean enabled;
    private Boolean locked;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Boolean firstLoginCompleted;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime passwordExpiresAt;
    private Boolean twoFactorEnabled;
}