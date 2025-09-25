package com.ejada.actuator.starter.endpoints;

import com.ejada.actuator.starter.metrics.SlaMetricsCalculator;
import com.ejada.actuator.starter.metrics.SlaMetricsCalculator.Result;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

@Endpoint(id = "sla-metrics")
public class SlaMetricsEndpoint {

  private final SlaMetricsCalculator calculator;

  public SlaMetricsEndpoint(SlaMetricsCalculator calculator) {
    this.calculator = calculator;
  }

  @ReadOperation
  public Map<String, Object> slaMetrics() {
    Result result = calculator.calculate();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("meter", result.getMeterName());
    response.put("totalRequests", result.getTotalRequests());
    response.put("successfulRequests", result.getSuccessfulRequests());
    response.put("failedRequests", result.getFailedRequests());
    response.put("sli", round(result.getSli()));
    response.put("sloTarget", round(result.getSloTarget()));
    response.put("slaTarget", round(result.getSlaTarget()));
    response.put("sloMet", result.isSloMet());
    response.put("slaMet", result.isSlaMet());
    response.put("errorBudget", round(result.getErrorBudget()));
    response.put("errorBudgetConsumed", round(result.getErrorBudgetConsumed()));
    response.put("errorBudgetRemaining", round(result.getErrorBudgetRemaining()));
    return response;
  }

  private double round(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }
}
