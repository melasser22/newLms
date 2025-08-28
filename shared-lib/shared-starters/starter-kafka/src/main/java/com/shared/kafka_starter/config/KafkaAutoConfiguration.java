package com.shared.kafka_starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shared.kafka_starter.props.KafkaProperties;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnClass(KafkaAdmin.class)
public class KafkaAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper kafkaObjectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    return om;
  }

  @Bean
  @ConditionalOnMissingBean
  public KafkaAdmin kafkaAdmin(KafkaProperties props) {
    Map<String, Object> cfg = new HashMap<>();
    cfg.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
    return new KafkaAdmin(cfg);
  }
}
