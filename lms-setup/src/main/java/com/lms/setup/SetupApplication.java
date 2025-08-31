package com.lms.setup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.lms")
@EnableCaching
public class SetupApplication {
  public static void main(String[] args) {
    SpringApplication.run(SetupApplication.class, args);
    
  }
  
}
