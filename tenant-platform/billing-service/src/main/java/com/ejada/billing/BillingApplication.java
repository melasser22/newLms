package com.ejada.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "Ejada Billing Service", version = "1.0"))
public class BillingApplication {
  private BillingApplication() { }

  public static void main(final String[] args) {
    SpringApplication.run(BillingApplication.class, args);
  }
}
