package com.ejada.email.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SendgridWebhookProperties.class)
public class EmailWebhookServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailWebhookServiceApplication.class, args);
  }
}
