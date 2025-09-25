
package com.ejada.actuator.starter.config;

import com.ejada.actuator.starter.endpoints.SlaMetricsEndpoint;
import com.ejada.actuator.starter.endpoints.WhoAmIEndpoint;
import com.ejada.actuator.starter.info.SharedInfoContributor;
import com.ejada.actuator.starter.metrics.CommonTagsCustomizer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

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
  @ConditionalOnClass({MeterRegistry.class, Endpoint.class})
  @ConditionalOnBean(MeterRegistry.class)
  @ConditionalOnAvailableEndpoint(endpoint = SlaMetricsEndpoint.class)
  @ConditionalOnProperty(prefix = "shared.actuator.sla-metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
  @ConditionalOnMissingBean
  public SlaMetricsEndpoint slaMetricsEndpoint(MeterRegistry registry, SharedActuatorProperties props) {
    return new SlaMetricsEndpoint(registry, props);
  }
}
