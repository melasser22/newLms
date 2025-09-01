package com.lms.tenant.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages="com.lms.tenant")
@OpenAPIDefinition(info = @Info(title = "Tenant API", version = "1.0"))
public class TenantApplication {
  public static void main(String[] args) {
    SpringApplication.run(TenantApplication.class, args);
  }
}