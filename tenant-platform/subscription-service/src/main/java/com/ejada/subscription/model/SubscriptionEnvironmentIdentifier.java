package com.ejada.subscription.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    private static final int IDENTIFIER_CD_LENGTH = 64;
    private static final int IDENTIFIER_VALUE_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_env_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionEnvId;

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "identifier_cd", length = IDENTIFIER_CD_LENGTH, nullable = false)
    private String identifierCd; // e.g., DB_ID

    @Column(name = "identifier_value", length = IDENTIFIER_VALUE_LENGTH, nullable = false)
    private String identifierValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static SubscriptionEnvironmentIdentifier ref(final Long id) {
        if (id == null) {
            return null;
        }
        SubscriptionEnvironmentIdentifier x = new SubscriptionEnvironmentIdentifier();
        x.setSubscriptionEnvId(id);
        return x;
    }
}
