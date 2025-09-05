package com.ejada.setup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableCaching
@OpenAPIDefinition(info = @Info(title = "Ejada Setup Service", version = "1.0"))
public class SetupApplication {
  public static void main(String[] args) {
    SpringApplication.run(SetupApplication.class, args);
    
  }
  
}
