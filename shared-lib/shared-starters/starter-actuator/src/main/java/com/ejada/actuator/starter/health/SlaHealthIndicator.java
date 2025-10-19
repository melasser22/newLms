package com.ejada.actuator.starter.health;

import com.ejada.actuator.starter.metrics.SlaMetricsCalculator;
import com.ejada.actuator.starter.metrics.SlaMetricsCalculator.Result;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

public class SlaHealthIndicator extends AbstractHealthIndicator {

  private final SlaMetricsCalculator calculator;
  private final Clock clock;

  public SlaHealthIndicator(SlaMetricsCalculator calculator) {
    this(calculator, Clock.systemUTC());
  }

  SlaHealthIndicator(SlaMetricsCalculator calculator, Clock clock) {
    this.calculator = calculator;
    this.clock = clock;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    Result result = calculator.calculate();
    boolean slaMet = result.isSlaMet();

    builder.status(Status.UP)
        .withDetail("sla_compliant", slaMet)
        .withDetail("availability_percent", round(result.getSli()))
        .withDetail("sli", round(result.getSli()))
        .withDetail("slo", round(result.getSloTarget()))
        .withDetail("slo_met", result.isSloMet())
        .withDetail("sla", round(result.getSlaTarget()))
        .withDetail("total_requests", result.getTotalRequests())
        .withDetail("successful_requests", result.getSuccessfulRequests())
        .withDetail("failed_requests", result.getFailedRequests())
        .withDetail("error_budget", round(result.getErrorBudget()))
        .withDetail("error_budget_consumed", round(result.getErrorBudgetConsumed()))
        .withDetail("error_budget_remaining", round(result.getErrorBudgetRemaining()))
        .withDetail("last_check", OffsetDateTime.now(clock));
  }

  private double round(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }
}
