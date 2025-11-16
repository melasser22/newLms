package com.ejada.webhook;

import com.ejada.webhook.config.SendGridWebhookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(SendGridWebhookProperties.class)
public class EmailWebhookServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailWebhookServiceApplication.class, args);
  }
}
