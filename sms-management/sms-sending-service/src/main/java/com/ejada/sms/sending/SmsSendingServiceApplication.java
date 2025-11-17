package com.ejada.sms.sending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.ejada.sms.sending")
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "SMS Sending Service", version = "1.0"))
public class SmsSendingServiceApplication {
  private SmsSendingServiceApplication() {}

  public static void main(String[] args) {
    SpringApplication.run(SmsSendingServiceApplication.class, args);
  }
}
