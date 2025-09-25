package com.ejada.actuator.starter.endpoints;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

@Endpoint(id = "sla-metrics")
public class SlaMetricsEndpoint {

  private final MeterRegistry meterRegistry;
  private final SharedActuatorProperties properties;

  public SlaMetricsEndpoint(MeterRegistry meterRegistry, SharedActuatorProperties properties) {
    this.meterRegistry = meterRegistry;
    this.properties = properties;
  }

  @ReadOperation
  public Map<String, Object> slaMetrics() {
    SharedActuatorProperties.SlaMetrics slaProps = properties.getSlaMetrics();
    Collection<Timer> timers = meterRegistry.find(slaProps.getMeterName()).timers();

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
    double sloTarget = slaProps.getSloTarget();
    double slaTarget = slaProps.getSlaTarget();
    double downtime = 100.0D - sli;
    double errorBudget = Math.max(0.0D, 100.0D - sloTarget);
    double errorBudgetConsumed = Math.min(errorBudget, downtime);
    double errorBudgetRemaining = Math.max(0.0D, errorBudget - downtime);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("meter", slaProps.getMeterName());
    response.put("totalRequests", totalRequests);
    response.put("successfulRequests", successfulRequests);
    response.put("failedRequests", failedRequests);
    response.put("sli", round(sli));
    response.put("sloTarget", round(sloTarget));
    response.put("slaTarget", round(slaTarget));
    response.put("sloMet", sli >= sloTarget);
    response.put("slaMet", sli >= slaTarget);
    response.put("errorBudget", round(errorBudget));
    response.put("errorBudgetConsumed", round(errorBudgetConsumed));
    response.put("errorBudgetRemaining", round(errorBudgetRemaining));
    return response;
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

  private double round(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }
}
