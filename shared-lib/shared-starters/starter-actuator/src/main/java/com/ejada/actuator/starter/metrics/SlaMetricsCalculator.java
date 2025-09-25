package com.ejada.actuator.starter.metrics;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;

/**
 * Utility component that inspects Micrometer timers to derive SLI/SLO/SLA metrics.
 */
public class SlaMetricsCalculator {

  private final MeterRegistry meterRegistry;
  private final SharedActuatorProperties properties;

  public SlaMetricsCalculator(MeterRegistry meterRegistry, SharedActuatorProperties properties) {
    this.meterRegistry = meterRegistry;
    this.properties = properties;
  }

  public Result calculate() {
    SharedActuatorProperties.SlaMetrics metricsProps = properties.getSlaMetrics();
    String meterName = metricsProps.getMeterName();

    Collection<Timer> timers = meterRegistry.find(meterName).timers();

    long totalRequests = 0L;
    long successfulRequests = 0L;
    for (Timer timer : timers) {
      long count = timer.count();
      totalRequests += count;
      if (isSuccessful(timer)) {
        successfulRequests += count;
      }
    }

    long failedRequests = Math.max(0L, totalRequests - successfulRequests);
    double sli = totalRequests == 0L ? 100.0D : ((double) successfulRequests / (double) totalRequests) * 100.0D;
    double sloTarget = metricsProps.getSloTarget();
    double slaTarget = metricsProps.getSlaTarget();
    double downtime = 100.0D - sli;
    double errorBudget = Math.max(0.0D, 100.0D - sloTarget);
    double errorBudgetConsumed = Math.min(errorBudget, downtime);
    double errorBudgetRemaining = Math.max(0.0D, errorBudget - downtime);

    return new Result(
        meterName,
        totalRequests,
        successfulRequests,
        failedRequests,
        sli,
        sloTarget,
        slaTarget,
        errorBudget,
        errorBudgetConsumed,
        errorBudgetRemaining);
  }

  private boolean isSuccessful(Timer timer) {
    String outcome = timer.getId().getTag("outcome");
    if (outcome != null) {
      if (outcome.equalsIgnoreCase("SERVER_ERROR") || outcome.equalsIgnoreCase("CLIENT_ERROR")) {
        return false;
      }
      return true;
    }
    String status = timer.getId().getTag("status");
    if (status == null || status.isBlank()) {
      return true;
    }
    char leading = status.charAt(0);
    return leading == '1' || leading == '2' || leading == '3';
  }

  public static final class Result {
    private final String meterName;
    private final long totalRequests;
    private final long successfulRequests;
    private final long failedRequests;
    private final double sli;
    private final double sloTarget;
    private final double slaTarget;
    private final double errorBudget;
    private final double errorBudgetConsumed;
    private final double errorBudgetRemaining;

    private Result(
        String meterName,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double sli,
        double sloTarget,
        double slaTarget,
        double errorBudget,
        double errorBudgetConsumed,
        double errorBudgetRemaining) {
      this.meterName = meterName;
      this.totalRequests = totalRequests;
      this.successfulRequests = successfulRequests;
      this.failedRequests = failedRequests;
      this.sli = sli;
      this.sloTarget = sloTarget;
      this.slaTarget = slaTarget;
      this.errorBudget = errorBudget;
      this.errorBudgetConsumed = errorBudgetConsumed;
      this.errorBudgetRemaining = errorBudgetRemaining;
    }

    public String getMeterName() {
      return meterName;
    }

    public long getTotalRequests() {
      return totalRequests;
    }

    public long getSuccessfulRequests() {
      return successfulRequests;
    }

    public long getFailedRequests() {
      return failedRequests;
    }

    public double getSli() {
      return sli;
    }

    public double getSloTarget() {
      return sloTarget;
    }

    public double getSlaTarget() {
      return slaTarget;
    }

    public double getErrorBudget() {
      return errorBudget;
    }

    public double getErrorBudgetConsumed() {
      return errorBudgetConsumed;
    }

    public double getErrorBudgetRemaining() {
      return errorBudgetRemaining;
    }

    public boolean isSloMet() {
      return sli >= sloTarget;
    }

    public boolean isSlaMet() {
      return sli >= slaTarget;
    }
  }
}

