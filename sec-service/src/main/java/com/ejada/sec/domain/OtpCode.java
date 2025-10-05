package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores generated OTP codes with expiration.
 * Used for EMAIL, SMS, and CONSOLE MFA methods.
 */
@Entity
@Table(name = "otp_codes",
       indexes = {
           @Index(name = "ix_otp_user", columnList = "user_id"),
           @Index(name = "ix_otp_code", columnList = "code_hash"),
           @Index(name = "ix_otp_expires", columnList = "expires_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User this OTP belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Hashed OTP code (never store plain text).
     */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    /**
     * Method this OTP was sent via.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", nullable = false, length = 20)
    private MfaMethod method;

    /**
     * When OTP was generated.
     */
    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private Instant generatedAt = Instant.now();

    /**
     * When OTP expires (typically 5 minutes).
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * When OTP was used (null if not used yet).
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Failed validation attempts for this OTP.
     */
    @Column(name = "failed_attempts")
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * Delivery details (email address, phone number, etc).
     */
    @Column(name = "sent_to", length = 255)
    private String sentTo;

    /**
     * Whether OTP was successfully delivered.
     */
    @Column(name = "delivered")
    @Builder.Default
    private Boolean delivered = false;

    /**
     * Delivery error message if failed.
     */
    @Column(name = "delivery_error", length = 500)
    private String deliveryError;

    /**
     * Checks if OTP is still valid (not expired, not used).
     */
    public boolean isValid() {
        return usedAt == null
            && expiresAt.isAfter(Instant.now())
            && Boolean.TRUE.equals(delivered);
    }

    /**
     * Marks OTP as used.
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }

    /**
     * Increments failed attempts.
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
}
