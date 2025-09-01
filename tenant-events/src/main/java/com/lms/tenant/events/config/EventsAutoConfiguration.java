package com.lms.tenant.events.config;

import com.lms.tenant.events.publisher.*;
import com.lms.tenant.events.support.TenantHeaderSupplier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@EnableConfigurationProperties(EventsProperties.class)
public class EventsAutoConfiguration {

  @Bean
  public OutboxService outboxService(JdbcTemplate jdbc) { return new OutboxService(jdbc); }

  @Bean
  @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
  public KafkaPublisher kafkaPublisher(org.springframework.kafka.core.KafkaTemplate<String, String> template,
                                       EventsProperties props,
                                       TenantHeaderSupplier tenantHeaderSupplier) {
    return new KafkaPublisher(template, props, tenantHeaderSupplier);
  }

  @Bean
  @ConditionalOnMissingBean(KafkaPublisher.class)
  public LogPublisher logPublisher(EventsProperties props, TenantHeaderSupplier tenantHeaderSupplier) {
    return new LogPublisher(props, tenantHeaderSupplier);
  }

  @Bean
  public OutboxPublisher outboxPublisher(OutboxService outboxService,
                                         EventsProperties props,
                                         java.util.Optional<KafkaPublisher> kafka,
                                         java.util.Optional<LogPublisher> log) {
    return new OutboxPublisher(outboxService, props, kafka.orElse(null), log.orElse(null));
  }

  @Bean
  @ConditionalOnMissingBean(TenantHeaderSupplier.class)
  public TenantHeaderSupplier defaultTenantHeaderSupplier() { return new TenantHeaderSupplier.Default(); }
}
