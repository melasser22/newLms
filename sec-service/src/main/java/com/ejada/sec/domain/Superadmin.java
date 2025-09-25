package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
  name = "superadmins",
  schema = "public",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_superadmins_username", columnNames = {"username"}),
    @UniqueConstraint(name = "ux_superadmins_email", columnNames = {"email"})
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Superadmin extends AuditableEntity {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 120, unique = true)
    private String username;
    
    @Column(nullable = false, length = 255, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(nullable = false, length = 64)
    @Builder.Default
    private String role = "EJADA_OFFICER";
    
    // Additional fields for first login tracking
    @Column(name = "first_login_completed", nullable = false)
    @Builder.Default
    private boolean firstLoginCompleted = false;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;
    
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private int failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    // Profile fields
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "two_factor_enabled")
    @Builder.Default
    private boolean twoFactorEnabled = false;
    
    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;
}