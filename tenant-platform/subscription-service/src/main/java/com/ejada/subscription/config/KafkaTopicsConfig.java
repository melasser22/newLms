package com.ejada.subscription.config;

import com.ejada.subscription.properties.SubscriptionKafkaTopicsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SubscriptionKafkaTopicsProperties.class)
public class KafkaTopicsConfig { }
