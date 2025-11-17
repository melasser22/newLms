package com.ejada.sms.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.ejada.sms.management")
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "SMS Management Gateway", version = "1.0"))
public class SmsManagementServiceApplication {
  private SmsManagementServiceApplication() { }

  public static void main(String[] args) {
    SpringApplication.run(SmsManagementServiceApplication.class, args);
  }
}
