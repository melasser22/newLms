package com.ejada.tenant.config;

import com.ejada.tenant.properties.TenantKafkaTopicsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantKafkaTopicsProperties.class)
public class KafkaTopicsConfig { }
