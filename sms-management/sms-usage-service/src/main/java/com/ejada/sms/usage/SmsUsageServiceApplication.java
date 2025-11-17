package com.ejada.sms.usage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "SMS Usage Service", version = "1.0"))
public class SmsUsageServiceApplication {
  private SmsUsageServiceApplication() { }

  public static void main(String[] args) {
    SpringApplication.run(SmsUsageServiceApplication.class, args);
  }
}
