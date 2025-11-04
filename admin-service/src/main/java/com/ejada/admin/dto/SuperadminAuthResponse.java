package com.ejada.admin.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuperadminAuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private String role;
    private List<String> permissions;
    
    // Additional fields for first login and password expiry
    private boolean requiresPasswordChange;
    private boolean passwordExpired;
    private String message;
}
