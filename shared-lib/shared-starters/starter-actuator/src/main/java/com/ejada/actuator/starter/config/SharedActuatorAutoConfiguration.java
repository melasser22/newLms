
package com.ejada.actuator.starter.config;

import com.ejada.actuator.starter.endpoints.SlaMetricsEndpoint;
import com.ejada.actuator.starter.endpoints.WhoAmIEndpoint;
import com.ejada.actuator.starter.health.SlaHealthIndicator;
import com.ejada.actuator.starter.metrics.SlaMetricsCalculator;
import com.ejada.actuator.starter.info.SharedInfoContributor;
import com.ejada.actuator.starter.metrics.CommonTagsCustomizer;
import com.ejada.actuator.starter.web.SlaReportController;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

@AutoConfiguration(after = InfoContributorAutoConfiguration.class)
@EnableConfigurationProperties(SharedActuatorProperties.class)
@PropertySource("classpath:/com/shared/actuator/starter/actuator-defaults.properties")
public class SharedActuatorAutoConfiguration {

  @Bean
  @ConditionalOnClass(MeterRegistry.class)
  @ConditionalOnMissingBean
  public CommonTagsCustomizer sharedCommonTagsCustomizer(Environment env, SharedActuatorProperties props) {
    return new CommonTagsCustomizer(env, props);
  }

  @Bean
  @ConditionalOnMissingBean(name = "sharedInfoContributor")
  public InfoContributor sharedInfoContributor() {
    return new SharedInfoContributor();
  }

  @Bean
  @ConditionalOnClass(InMemoryHttpExchangeRepository.class)
  @ConditionalOnProperty(prefix = "shared.actuator.http-exchanges", name = "enabled", havingValue = "true", matchIfMissing = true)
  public InMemoryHttpExchangeRepository httpExchangeRepository(SharedActuatorProperties props) {
    InMemoryHttpExchangeRepository repo = new InMemoryHttpExchangeRepository();
    repo.setCapacity(props.getHttpExchanges().getCapacity());
    return repo;
  }

  @Bean
  @ConditionalOnMissingBean
  public WhoAmIEndpoint whoAmIEndpoint() {
    return new WhoAmIEndpoint();
  }

  @Bean
  @ConditionalOnClass(RestController.class)
  @ConditionalOnProperty(prefix = "shared.actuator.sla-report", name = "enabled", havingValue = "true", matchIfMissing = true)
  @ConditionalOnMissingBean
  public SlaReportController slaReportController(
      ObjectProvider<HealthEndpoint> healthEndpoint,
      ObjectProvider<HealthContributorRegistry> registry,
      ObjectProvider<SlaHealthIndicator> indicator) {
    return new SlaReportController(healthEndpoint, registry, indicator);
  }

  @Bean
  @ConditionalOnClass(MeterRegistry.class)
  @ConditionalOnMissingBean
  public SlaMetricsCalculator slaMetricsCalculator(
      MeterRegistry meterRegistry, SharedActuatorProperties properties) {
    return new SlaMetricsCalculator(meterRegistry, properties);
  }

  @Bean
  @ConditionalOnClass(MeterRegistry.class)
  @ConditionalOnBean(SlaMetricsCalculator.class)
  @ConditionalOnMissingBean
  public SlaMetricsEndpoint slaMetricsEndpoint(SlaMetricsCalculator calculator) {
    return new SlaMetricsEndpoint(calculator);
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "shared.actuator.sla-report",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  @ConditionalOnBean(SlaMetricsCalculator.class)
  @ConditionalOnMissingBean(name = "slaHealthIndicator")
  public SlaHealthIndicator slaHealthIndicator(SlaMetricsCalculator calculator) {
    return new SlaHealthIndicator(calculator);

  }
}
