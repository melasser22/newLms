package com.ejada.tenant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "tenant_health_score",
        indexes = {
                @Index(name = "idx_tenant_health_score_tenant", columnList = "tenant_id, evaluated_at"),
                @Index(name = "idx_tenant_health_score_created", columnList = "created_at")
        })
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TenantHealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "tenant_health_score_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", length = 32, nullable = false)
    private TenantHealthRiskCategory riskCategory;

    @Column(name = "feature_adoption_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal featureAdoptionRate;

    @Column(name = "login_frequency_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal loginFrequencyScore;

    @Column(name = "user_engagement_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal userEngagementScore;

    @Column(name = "usage_trend_percent", precision = 6, scale = 2, nullable = false)
    private BigDecimal usageTrendPercent;

    @Column(name = "support_ticket_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal supportTicketScore;

    @Column(name = "payment_history_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal paymentHistoryScore;

    @Column(name = "api_health_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal apiHealthScore;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (evaluatedAt == null) {
            evaluatedAt = OffsetDateTime.now();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
