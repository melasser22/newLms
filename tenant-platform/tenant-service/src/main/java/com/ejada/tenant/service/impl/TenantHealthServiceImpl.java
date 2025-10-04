package com.ejada.tenant.service.impl;

import com.ejada.common.dto.BaseResponse;
import com.ejada.tenant.dto.TenantHealthScoreRes;
import com.ejada.tenant.dto.event.TenantHealthScoreCalculatedEvent;
import com.ejada.tenant.model.OutboxEvent;
import com.ejada.tenant.model.Tenant;
import com.ejada.tenant.model.TenantHealthRiskCategory;
import com.ejada.tenant.model.TenantHealthScore;
import com.ejada.tenant.repository.OutboxEventRepository;
import com.ejada.tenant.repository.TenantHealthScoreRepository;
import com.ejada.tenant.repository.TenantRepository;
import com.ejada.tenant.service.TenantHealthService;
import com.ejada.tenant.service.health.TenantHealthMetrics;
import com.ejada.tenant.service.health.TenantHealthMetricsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class TenantHealthServiceImpl implements TenantHealthService {

    private static final String AGGREGATE_TYPE = "TENANT";
    private static final String EVENT_TYPE = "TenantHealthScoreCalculated";
    private static final Map<MetricKey, Double> WEIGHTS = createWeights();

    private final TenantRepository tenantRepository;
    private final TenantHealthScoreRepository tenantHealthScoreRepository;
    private final TenantHealthMetricsProvider metricsProvider;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public TenantHealthServiceImpl(
            final TenantRepository tenantRepository,
            final TenantHealthScoreRepository tenantHealthScoreRepository,
            final TenantHealthMetricsProvider metricsProvider,
            final OutboxEventRepository outboxEventRepository,
            final ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.tenantHealthScoreRepository = tenantHealthScoreRepository;
        this.metricsProvider = metricsProvider;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper.copy();
    }

    @Override
    public BaseResponse<TenantHealthScoreRes> getHealthScore(final Integer tenantId) {
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + tenantId));

        TenantHealthScore latest = tenantHealthScoreRepository
                .findFirstByTenant_IdOrderByEvaluatedAtDesc(tenant.getId())
                .orElseGet(() -> persistHealthScore(tenant));

        return BaseResponse.success("Tenant health score fetched", mapToResponse(latest));
    }

    @Override
    public TenantHealthScoreRes calculateAndStoreHealthScore(final Integer tenantId) {
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant " + tenantId));
        TenantHealthScore score = persistHealthScore(tenant);
        return mapToResponse(score);
    }

    private TenantHealthScore persistHealthScore(final Tenant tenant) {
        TenantHealthMetrics metrics = metricsOrDefault(metricsProvider.collect(tenant.getId()));
        int score = calculateScore(metrics);
        TenantHealthRiskCategory riskCategory = determineRiskCategory(score);
        TenantHealthScore entity = TenantHealthScore.builder()
                .tenant(Tenant.ref(tenant.getId()))
                .score(score)
                .riskCategory(riskCategory)
                .featureAdoptionRate(toPercentage(metrics.featureAdoptionRate()))
                .loginFrequencyScore(toPercentage(metrics.loginFrequencyScore()))
                .userEngagementScore(toPercentage(metrics.userEngagementScore()))
                .usageTrendPercent(toTrendPercentage(metrics.usageTrendScore()))
                .supportTicketScore(toPercentage(metrics.supportTicketScore()))
                .paymentHistoryScore(toPercentage(metrics.paymentHistoryScore()))
                .apiHealthScore(toPercentage(metrics.apiHealthScore()))
                .evaluatedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .build();

        TenantHealthScore saved = tenantHealthScoreRepository.save(entity);
        publishOutboxEvent(saved);
        return saved;
    }

    private int calculateScore(final TenantHealthMetrics metrics) {
        double adoption = clamp(metrics.featureAdoptionRate());
        double login = clamp(metrics.loginFrequencyScore());
        double engagement = clamp(metrics.userEngagementScore());
        double usage = clamp((metrics.usageTrendScore() + 1.0) / 2.0);
        double support = clamp(metrics.supportTicketScore());
        double payment = clamp(metrics.paymentHistoryScore());
        double api = clamp(metrics.apiHealthScore());

        double weighted = adoption * weight(MetricKey.ADOPTION)
                + login * weight(MetricKey.LOGIN)
                + engagement * weight(MetricKey.ENGAGEMENT)
                + usage * weight(MetricKey.USAGE)
                + support * weight(MetricKey.SUPPORT)
                + payment * weight(MetricKey.PAYMENT)
                + api * weight(MetricKey.API);

        return (int) Math.round(weighted * 100);
    }

    private TenantHealthMetrics metricsOrDefault(final TenantHealthMetrics metrics) {
        return metrics == null ? TenantHealthMetrics.empty() : metrics;
    }

    private TenantHealthRiskCategory determineRiskCategory(final int score) {
        if (score < 40) {
            return TenantHealthRiskCategory.AT_RISK;
        } else if (score < 65) {
            return TenantHealthRiskCategory.NEEDS_ATTENTION;
        } else if (score < 85) {
            return TenantHealthRiskCategory.HEALTHY;
        }
        return TenantHealthRiskCategory.CHAMPION;
    }

    private BigDecimal toPercentage(final double value) {
        double clamped = clamp(value);
        return BigDecimal.valueOf(clamped * 100).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toTrendPercentage(final double value) {
        double clamped = Math.max(-1.0, Math.min(1.0, value));
        return BigDecimal.valueOf(clamped * 100).setScale(2, RoundingMode.HALF_UP);
    }

    private double clamp(final double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }

    private double weight(final MetricKey key) {
        return WEIGHTS.getOrDefault(key, 0d);
    }

    private TenantHealthScoreRes mapToResponse(final TenantHealthScore score) {
        Integer tenantId = score.getTenant() != null ? score.getTenant().getId() : null;
        return new TenantHealthScoreRes(
                tenantId,
                score.getScore(),
                score.getRiskCategory(),
                score.getFeatureAdoptionRate(),
                score.getLoginFrequencyScore(),
                score.getUserEngagementScore(),
                score.getUsageTrendPercent(),
                score.getSupportTicketScore(),
                score.getPaymentHistoryScore(),
                score.getApiHealthScore(),
                score.getEvaluatedAt()
        );
    }

    private void publishOutboxEvent(final TenantHealthScore saved) {
        TenantHealthScoreCalculatedEvent event = new TenantHealthScoreCalculatedEvent(
                saved.getTenant().getId(),
                saved.getScore(),
                saved.getRiskCategory(),
                saved.getFeatureAdoptionRate(),
                saved.getLoginFrequencyScore(),
                saved.getUserEngagementScore(),
                saved.getUsageTrendPercent(),
                saved.getSupportTicketScore(),
                saved.getPaymentHistoryScore(),
                saved.getApiHealthScore(),
                saved.getEvaluatedAt()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(AGGREGATE_TYPE)
                    .aggregateId(String.valueOf(saved.getTenant().getId()))
                    .eventType(EVENT_TYPE)
                    .payload(payload)
                    .createdAt(OffsetDateTime.now())
                    .published(Boolean.FALSE)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize tenant health score event for tenant {}", saved.getTenant().getId(), ex);
            throw new IllegalStateException("Failed to serialize tenant health score event", ex);
        }
    }

    private static Map<MetricKey, Double> createWeights() {
        Map<MetricKey, Double> map = new EnumMap<>(MetricKey.class);
        map.put(MetricKey.ADOPTION, 0.25);
        map.put(MetricKey.LOGIN, 0.15);
        map.put(MetricKey.ENGAGEMENT, 0.15);
        map.put(MetricKey.USAGE, 0.15);
        map.put(MetricKey.SUPPORT, 0.1);
        map.put(MetricKey.PAYMENT, 0.1);
        map.put(MetricKey.API, 0.1);
        return Map.copyOf(map);
    }

    private enum MetricKey {
        ADOPTION,
        LOGIN,
        ENGAGEMENT,
        USAGE,
        SUPPORT,
        PAYMENT,
        API
    }
}
