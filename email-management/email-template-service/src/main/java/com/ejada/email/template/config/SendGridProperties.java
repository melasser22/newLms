package com.ejada.email.template.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email.sendgrid")
public record SendGridProperties(
    boolean sandboxMode,
    Duration timeout,
    String webhookPublicKey,
    String defaultFromEmail,
    String defaultFromName) {}
