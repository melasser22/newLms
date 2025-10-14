package com.ejada.gateway.bff;

import com.ejada.gateway.bff.MobileTenantDashboardResponse.MobileBillingSummary;
import com.ejada.gateway.bff.MobileTenantDashboardResponse.MobileMetric;
import com.ejada.gateway.aggregate.TenantDashboardAggregateResponse;
import com.ejada.gateway.aggregate.TenantDashboardAggregationService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Converts the canonical tenant dashboard aggregation into a compact, mobile friendly payload by
 * extracting a curated subset of metrics and flattening common fields.
 */
@Service
public class MobileTenantDashboardService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MobileTenantDashboardService.class);

  private final TenantDashboardAggregationService aggregationService;

  public MobileTenantDashboardService(TenantDashboardAggregationService aggregationService) {
    this.aggregationService = Objects.requireNonNull(aggregationService, "aggregationService");
  }

  public Mono<MobileTenantDashboardResponse> build(Integer tenantId) {
    return aggregationService.aggregate(tenantId)
        .map(this::transform);
  }

  private MobileTenantDashboardResponse transform(TenantDashboardAggregateResponse aggregate) {
    JsonNode tenant = aggregate.tenant();
    String tenantId = safeText(tenant, "id");
    String tenantName = safeText(tenant, "name");
    String plan = safeText(aggregate.subscriptions(), "plan");
    String status = safeText(tenant, "status");

    MobileBillingSummary billingSummary = extractBilling(aggregate.billing());
    List<MobileMetric> metrics = extractHighlights(aggregate);

    return new MobileTenantDashboardResponse(tenantId,
        tenantName,
        plan,
        status,
        billingSummary,
        metrics,
        aggregate.warnings());
  }

  private MobileBillingSummary extractBilling(JsonNode billingNode) {
    if (billingNode == null || billingNode.isNull()) {
      return new MobileBillingSummary("USD", 0.0, 0.0);
    }
    double spend = safeDouble(billingNode, "monthlySpend");
    double usage = safeDouble(billingNode, "usagePercentage");
    String currency = safeText(billingNode, "currency");
    if (currency == null || currency.isBlank()) {
      currency = "USD";
    }
    return new MobileBillingSummary(currency, spend, usage);
  }

  private List<MobileMetric> extractHighlights(TenantDashboardAggregateResponse aggregate) {
    List<MobileMetric> metrics = new ArrayList<>();
    JsonNode subscriptions = aggregate.subscriptions();
    if (subscriptions != null && subscriptions.has("activeCount")) {
      metrics.add(new MobileMetric("Active Subscriptions",
          String.valueOf(subscriptions.path("activeCount").asInt()),
          trend(subscriptions.path("activeTrend"))));
    }

    JsonNode tenant = aggregate.tenant();
    if (tenant != null && tenant.has("healthScore")) {
      metrics.add(new MobileMetric("Health Score",
          String.valueOf(tenant.path("healthScore").asInt()),
          trend(tenant.path("healthTrend"))));
    }

    JsonNode billing = aggregate.billing();
    if (billing != null && billing.has("monthlySpend")) {
      metrics.add(new MobileMetric("Monthly Spend",
          formattedCurrency(billing),
          trend(billing.path("spendTrend"))));
    }

    if (metrics.isEmpty()) {
      LOGGER.debug("No highlight metrics available for tenant {}", safeText(tenant, "id"));
    }
    return List.copyOf(metrics);
  }

  private String formattedCurrency(JsonNode billing) {
    double value = safeDouble(billing, "monthlySpend");
    String currency = safeText(billing, "currency");
    if (currency == null || currency.isBlank()) {
      currency = "USD";
    }
    return currency + " " + String.format("%.2f", value);
  }

  private String trend(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return "flat";
    }
    return node.asText("flat");
  }

  private String safeText(JsonNode node, String field) {
    if (node == null || node.isNull() || field == null) {
      return null;
    }
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }

  private double safeDouble(JsonNode node, String field) {
    if (node == null || node.isNull() || field == null) {
      return 0.0;
    }
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? 0.0 : value.asDouble();
  }
}
