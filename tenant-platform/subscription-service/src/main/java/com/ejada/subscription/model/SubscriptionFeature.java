package com.ejada.subscription.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "subscription_feature",
    uniqueConstraints = @UniqueConstraint(name = "ux_sf", columnNames = {"subscription_id","feature_cd"}),
    indexes = @Index(name = "idx_sf_sub", columnList = "subscription_id")
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_feature_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionFeatureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "feature_cd", length = 128, nullable = false)
    private String featureCd;

    @Column(name = "feature_count")
    private Integer featureCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static SubscriptionFeature ref(Long id) {
        if (id == null) return null;
        SubscriptionFeature x = new SubscriptionFeature();
        x.setSubscriptionFeatureId(id);
        return x;
    }
}
