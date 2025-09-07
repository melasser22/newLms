package com.ejada.subscription.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "subscription_environment_identifier",
    uniqueConstraints = @UniqueConstraint(name = "ux_sei", columnNames = {"subscription_id","identifier_cd"}),
    indexes = @Index(name = "idx_sei_sub", columnList = "subscription_id")
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionEnvironmentIdentifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_env_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionEnvId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "identifier_cd", length = 64, nullable = false)
    private String identifierCd; // e.g., DB_ID

    @Column(name = "identifier_value", length = 512, nullable = false)
    private String identifierValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static SubscriptionEnvironmentIdentifier ref(Long id) {
        if (id == null) return null;
        SubscriptionEnvironmentIdentifier x = new SubscriptionEnvironmentIdentifier();
        x.setSubscriptionEnvId(id);
        return x;
    }
}
