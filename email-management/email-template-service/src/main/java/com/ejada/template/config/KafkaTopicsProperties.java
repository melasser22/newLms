package com.ejada.template.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email.kafka-topics")
public record KafkaTopicsProperties(
    String emailSend,
    String emailSendRetry,
    String emailSendDlq,
    String webhookEvents,
    String emailEvents) {}
