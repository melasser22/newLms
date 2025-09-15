package com.ejada.subscription.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    private static final int SERVICE_CD_LENGTH = 128;
    private static final int SERVICE_NAME_LENGTH = 256;
    private static final int PRICE_PRECISION = 18;
    private static final int PRICE_SCALE = 4;
    private static final int CURRENCY_LENGTH = 3;
    private static final int FLAG_LENGTH = 1;
    private static final int PAYMENT_TYPE_LENGTH = 32;

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

    @Column(name = "service_cd", length = SERVICE_CD_LENGTH, nullable = false)
    private String serviceCd;

    @Column(name = "service_name_en", length = SERVICE_NAME_LENGTH, nullable = false)
    private String serviceNameEn;

    @Column(name = "service_name_ar", length = SERVICE_NAME_LENGTH, nullable = false)
    private String serviceNameAr;

    @Column(name = "service_desc_en", columnDefinition = "text")
    private String serviceDescEn;

    @Column(name = "service_desc_ar", columnDefinition = "text")
    private String serviceDescAr;

    @Column(name = "service_price", precision = PRICE_PRECISION, scale = PRICE_SCALE)
    private BigDecimal servicePrice;

    @Column(name = "total_amount", precision = PRICE_PRECISION, scale = PRICE_SCALE)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = CURRENCY_LENGTH)
    private String currency;

    @Convert(converter = YesNoBooleanConverter.class)
    @Column(name = "is_countable", length = FLAG_LENGTH)
    private Boolean isCountable = Boolean.FALSE;

    @Column(name = "requested_count")
    private Long requestedCount;

    @Column(name = "payment_type_cd", length = PAYMENT_TYPE_LENGTH)
    private String paymentTypeCd; // ONE_TIME_FEES | WITH_INSTALLMENT

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public static SubscriptionAdditionalService ref(final Long id) {
        if (id == null) {
            return null;
        }
        SubscriptionAdditionalService x = new SubscriptionAdditionalService();
        x.setSubscriptionAdditionalServiceId(id);
        return x;
    }
}
