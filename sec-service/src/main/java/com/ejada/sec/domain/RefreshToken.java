package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
  name = "refresh_tokens",
  uniqueConstraints = @UniqueConstraint(name = "ux_refresh_tokens_token", columnNames = "token"),
  indexes = {
    @Index(name="ix_refresh_tokens_user", columnList="user_id"),
    @Index(name="ix_refresh_tokens_expiry", columnList="expires_at")
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String token;

    @Column(name = "issued_at",  nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "rotated_from", length = 255)
    private String rotatedFrom;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;
}
