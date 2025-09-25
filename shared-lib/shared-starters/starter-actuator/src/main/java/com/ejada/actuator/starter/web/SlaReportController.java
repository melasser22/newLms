package com.ejada.actuator.starter.web;

import java.util.LinkedHashMap;

import java.util.Map;
import com.ejada.actuator.starter.health.SlaHealthIndicator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sla")
public class SlaReportController {

  private static final String INDICATOR_NAME = "slaHealthIndicator";

  private final ObjectProvider<HealthEndpoint> healthEndpoint;
  private final ObjectProvider<HealthContributorRegistry> registry;
  private final ObjectProvider<SlaHealthIndicator> indicator;

  public SlaReportController(
      ObjectProvider<HealthEndpoint> healthEndpoint,
      ObjectProvider<HealthContributorRegistry> registry,
      ObjectProvider<SlaHealthIndicator> indicator) {
    this.healthEndpoint = healthEndpoint;
    this.registry = registry;
    this.indicator = indicator;
  }

  @GetMapping("/report")
  public Map<String, Object> report() {
    HealthComponent component = resolveIndicator();
    Map<String, Object> body = new LinkedHashMap<>();
    Status status = component != null ? component.getStatus() : Status.UNKNOWN;
    body.put("status", status != null ? status.getCode() : Status.UNKNOWN.getCode());

    Map<String, Object> components = new LinkedHashMap<>();
    components.put(INDICATOR_NAME, component != null ? toMap(component) : fallbackComponent());
    body.put("components", components);

    return body;
  }

  private HealthComponent resolveIndicator() {
    HealthContributorRegistry registry = this.registry.getIfAvailable();
    if (registry != null) {
      HealthContributor contributor = registry.getContributor(INDICATOR_NAME);
      if (contributor instanceof HealthIndicator indicator) {
        return indicator.getHealth(true);
      }
      if (contributor instanceof CompositeHealthContributor compositeContributor) {
        for (var named : compositeContributor) {
          if (INDICATOR_NAME.equals(named.getName()) && named.getContributor() instanceof HealthIndicator indicator) {
            return indicator.getHealth(true);
          }
        }
      }
    }

    SlaHealthIndicator direct = indicator.getIfAvailable();
    if (direct != null) {
      return direct.getHealth(true);
    }

    HealthEndpoint endpoint = healthEndpoint.getIfAvailable();
    if (endpoint != null) {
      HealthComponent root = endpoint.health();
      if (root instanceof CompositeHealth composite) {
        return composite.getComponents().get(INDICATOR_NAME);
      }
      return root;
    }
    return null;
  }

  private Map<String, Object> toMap(HealthComponent component) {
    Map<String, Object> result = new LinkedHashMap<>();
    Status status = component.getStatus();
    result.put("status", status != null ? status.getCode() : Status.UNKNOWN.getCode());

    if (component instanceof Health health) {
      if (!health.getDetails().isEmpty()) {
        result.put("details", new LinkedHashMap<>(health.getDetails()));
      }
    }

    if (component instanceof CompositeHealth composite && !composite.getComponents().isEmpty()) {
      Map<String, Object> children = new LinkedHashMap<>();
      for (Map.Entry<String, HealthComponent> entry : composite.getComponents().entrySet()) {
        children.put(entry.getKey(), toMap(entry.getValue()));
      }
      result.put("components", children);
    }

    return result;
  }

  private Map<String, Object> fallbackComponent() {
    Map<String, Object> fallback = new LinkedHashMap<>();
    fallback.put("status", Status.UNKNOWN.getCode());
    return fallback;

  }
}
