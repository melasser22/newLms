package com.ejada.sms.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.ejada.sms.webhook")
@OpenAPIDefinition(info = @Info(title = "SMS Webhook Service", version = "1.0"))
public class SmsWebhookServiceApplication {
  private SmsWebhookServiceApplication() { }

  public static void main(String[] args) {
    SpringApplication.run(SmsWebhookServiceApplication.class, args);
  }
}
