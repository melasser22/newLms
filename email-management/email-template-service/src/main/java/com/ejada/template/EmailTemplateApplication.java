package com.ejada.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.ejada.template")
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "NE Email Template  Service", version = "1.0"))
public class EmailTemplateApplication {
  private EmailTemplateApplication() { }

  public static void main(final String[] args) {
    SpringApplication.run(EmailTemplateApplication.class, args);
  }
}
