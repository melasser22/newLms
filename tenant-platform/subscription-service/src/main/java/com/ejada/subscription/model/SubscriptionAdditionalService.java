package com.ejada.subscription.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "subscription_additional_service",
    indexes = @Index(name = "idx_sas_sub", columnList = "subscription_id")
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionAdditionalService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_additional_service_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long subscriptionAdditionalServiceId;

    @SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "product_additional_service_id", nullable = false)
    private Long productAdditionalServiceId;

    @Column(name = "service_cd", length = 128, nullable = false)
    private String serviceCd;

    @Column(name = "service_name_en", length = 256, nullable = false)
    private String serviceNameEn;

    @Column(name = "service_name_ar", length = 256, nullable = false)
    private String serviceNameAr;

    @Column(name = "service_desc_en", columnDefinition = "text")
    private String serviceDescEn;

    @Column(name = "service_desc_ar", columnDefinition = "text")
    private String serviceDescAr;

    @Column(name = "service_price", precision = 18, scale = 4)
    private BigDecimal servicePrice;

    @Column(name = "total_amount", precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "is_countable", length = 1)
    private Boolean isCountable = Boolean.FALSE;

    @Column(name = "requested_count")
    private Long requestedCount;

    @Column(name = "payment_type_cd", length = 32)
    private String paymentTypeCd; // ONE_TIME_FEES | WITH_INSTALLMENT

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static SubscriptionAdditionalService ref(Long id) {
        if (id == null) return null;
        SubscriptionAdditionalService x = new SubscriptionAdditionalService();
        x.setSubscriptionAdditionalServiceId(id);
        return x;
    }
}
