package com.ejada.sms.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.ejada.sms.template")
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "SMS Template Service", version = "1.0"))
public class SmsTemplateApplication {
  private SmsTemplateApplication() { }

  public static void main(String[] args) {
    SpringApplication.run(SmsTemplateApplication.class, args);
  }
}
