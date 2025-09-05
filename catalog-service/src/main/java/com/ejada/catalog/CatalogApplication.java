package com.ejada.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "Ejada Catalog Service", version = "1.0"))
public class CatalogApplication {
  private CatalogApplication() {}

  public static void main(final String[] args) {
    SpringApplication.run(CatalogApplication.class, args);
  }
}
