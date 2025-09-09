package com.ejada.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "Ejada Tenant Service", version = "1.0"))
public class TenantApplication {
  private TenantApplication() { }

  public static void main(final String[] args) {
    SpringApplication.run(TenantApplication.class, args);
  }
}
