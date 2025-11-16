package com.ejada.sec.dto;

import java.util.List;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
	 private String accessToken;
	    private String refreshToken;
	    @Builder.Default
	    private String tokenType = "Bearer";
	    private long expiresInSeconds;
            private String role;
            @Builder.Default
            private List<String> roles = List.of();
            @Builder.Default
            private List<String> permissions = List.of();
	    
	    // Additional fields for first login and password expiry
	    private boolean requiresPasswordChange;
	    private boolean passwordExpired;
	    private String message;
}
