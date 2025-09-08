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
    name = "subscription_product_property",
    uniqueConstraints = @UniqueConstraint(name = "ux_spp", columnNames = {"subscription_id","property_cd"}),
    indexes = @Index(name = "idx_spp_sub", columnList = "subscription_id")
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionProductProperty {

    private static final int PROPERTY_CD_LENGTH = 128;
    private static final int PROPERTY_VALUE_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_product_property_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionProductPropertyId;

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "property_cd", length = PROPERTY_CD_LENGTH, nullable = false)
    private String propertyCd;

    @Column(name = "property_value", length = PROPERTY_VALUE_LENGTH, nullable = false)
    private String propertyValue;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static SubscriptionProductProperty ref(final Long id) {
        if (id == null) {
            return null;
        }
        SubscriptionProductProperty x = new SubscriptionProductProperty();
        x.setSubscriptionProductPropertyId(id);
        return x;
    }
}
