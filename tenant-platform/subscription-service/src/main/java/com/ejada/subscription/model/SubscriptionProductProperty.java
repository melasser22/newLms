package com.ejada.subscription.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "subscription_product_property",
    uniqueConstraints = @UniqueConstraint(name = "ux_spp", columnNames = {"subscription_id","property_cd"}),
    indexes = @Index(name = "idx_spp_sub", columnList = "subscription_id")
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionProductProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_product_property_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionProductPropertyId;

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "property_cd", length = 128, nullable = false)
    private String propertyCd;

    @Column(name = "property_value", length = 2048, nullable = false)
    private String propertyValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static SubscriptionProductProperty ref(Long id) {
        if (id == null) return null;
        SubscriptionProductProperty x = new SubscriptionProductProperty();
        x.setSubscriptionProductPropertyId(id);
        return x;
    }
}
