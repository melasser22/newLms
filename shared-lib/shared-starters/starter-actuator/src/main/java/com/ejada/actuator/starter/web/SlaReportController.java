package com.ejada.actuator.starter.web;

import com.ejada.actuator.starter.config.SharedActuatorProperties;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sla")
public class SlaReportController {

  private final ObjectProvider<BuildProperties> buildProperties;
  private final ObjectProvider<GitProperties> gitProperties;
  private final ObjectProvider<HealthEndpoint> healthEndpoint;
  private final ObjectProvider<InfoEndpoint> infoEndpoint;
  private final Environment environment;
  private final SharedActuatorProperties properties;
  private final Clock clock;

  public SlaReportController(
      ObjectProvider<BuildProperties> buildProperties,
      ObjectProvider<GitProperties> gitProperties,
      ObjectProvider<HealthEndpoint> healthEndpoint,
      ObjectProvider<InfoEndpoint> infoEndpoint,
      Environment environment,
      SharedActuatorProperties properties) {
    this(buildProperties, gitProperties, healthEndpoint, infoEndpoint, environment, properties,
        Clock.systemUTC());
  }

  SlaReportController(
      ObjectProvider<BuildProperties> buildProperties,
      ObjectProvider<GitProperties> gitProperties,
      ObjectProvider<HealthEndpoint> healthEndpoint,
      ObjectProvider<InfoEndpoint> infoEndpoint,
      Environment environment,
      SharedActuatorProperties properties,
      Clock clock) {
    this.buildProperties = buildProperties;
    this.gitProperties = gitProperties;
    this.healthEndpoint = healthEndpoint;
    this.infoEndpoint = infoEndpoint;
    this.environment = environment;
    this.properties = properties;
    this.clock = clock;
  }

  @GetMapping("/report")
  public SlaReport report() {
    var runtimeBean = ManagementFactory.getRuntimeMXBean();
    Duration uptime = Duration.ofMillis(runtimeBean.getUptime());
    OffsetDateTime generatedAt = OffsetDateTime.now(clock);
    OffsetDateTime startedAt = generatedAt.minus(uptime);

    String serviceName = environment.getProperty("spring.application.name", "application");
    List<String> profiles = resolveProfiles();

    BuildProperties build = buildProperties.getIfAvailable();
    GitProperties git = gitProperties.getIfAvailable();

    SlaReport.Build buildInfo = (build != null || git != null)
        ? new SlaReport.Build(
            build != null ? build.getVersion() : null,
            build != null ? build.getTime() : null,
            git != null ? git.getShortCommitId() : null,
            git != null ? git.getBranch() : null)
        : null;

    String[] host = resolveHost();
    SlaReport.Runtime runtime = new SlaReport.Runtime(
        runtimeBean.getPid(),
        host[0],
        host[1],
        envOrNull("REGION"),
        envOrNull("ZONE"),
        envOrNull("POD_NAME"),
        envOrNull("NODE_NAME"));

    HealthComponent healthComponent = null;
    Status status = Status.UNKNOWN;
    Map<String, Object> healthDetails = null;
    HealthEndpoint healthEndpoint = this.healthEndpoint.getIfAvailable();
    if (healthEndpoint != null) {
      healthComponent = healthEndpoint.health();
    }
    if (healthComponent != null) {
      status = healthComponent.getStatus();
      healthDetails = nullIfEmpty(asHealthMap(healthComponent));
    }

    Map<String, Object> infoDetails = null;
    InfoEndpoint infoEndpoint = this.infoEndpoint.getIfAvailable();
    if (infoEndpoint != null) {
      infoDetails = nullIfEmpty(new LinkedHashMap<>(infoEndpoint.info()));
    }

    SharedActuatorProperties.SlaReport sla = properties.getSlaReport();
    String owner = blankToNull(sla.getOwner());
    String contact = blankToNull(sla.getContact());
    String description = blankToNull(sla.getDescription());
    SlaReport.Metadata metadata =
        (owner != null || contact != null || description != null)
            ? new SlaReport.Metadata(owner, contact, description)
            : null;

    return new SlaReport(
        serviceName,
        profiles,
        status != null ? status.getCode() : Status.UNKNOWN.getCode(),
        generatedAt,
        uptime,
        startedAt,
        buildInfo,
        runtime,
        metadata,
        infoDetails,
        healthDetails);
  }

  private List<String> resolveProfiles() {
    List<String> active = Arrays.stream(environment.getActiveProfiles())
        .filter(StringUtils::hasText)
        .toList();
    if (!active.isEmpty()) {
      return active;
    }
    return Arrays.stream(environment.getDefaultProfiles())
        .filter(StringUtils::hasText)
        .toList();
  }

  private Map<String, Object> asHealthMap(HealthComponent component) {
    Map<String, Object> result = new LinkedHashMap<>();
    Status componentStatus = component.getStatus();
    if (componentStatus != null) {
      result.put("status", componentStatus.getCode());
    }
    if (component instanceof Health health && !health.getDetails().isEmpty()) {
      result.put("details", new LinkedHashMap<>(health.getDetails()));
    }
    if (component instanceof CompositeHealth composite && !composite.getComponents().isEmpty()) {
      Map<String, Object> components = new LinkedHashMap<>();
      composite.getComponents()
          .forEach((name, child) -> components.put(name, asHealthMap(child)));
      result.put("components", components);
    }
    return result;
  }

  private String[] resolveHost() {
    try {
      InetAddress host = InetAddress.getLocalHost();
      return new String[] {host.getHostName(), host.getHostAddress()};
    } catch (Exception ex) {
      return new String[] {"unknown", "unknown"};
    }
  }

  private Map<String, Object> nullIfEmpty(Map<String, Object> source) {
    if (source == null || source.isEmpty()) {
      return null;
    }
    return source;
  }

  private String envOrNull(String key) {
    return blankToNull(System.getenv(key));
  }

  private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
