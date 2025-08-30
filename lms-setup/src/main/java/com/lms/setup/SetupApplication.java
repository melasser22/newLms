package com.lms.setup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// TODO: The database configuration has been updated to use Flyway for schema management (`ddl-auto: validate`).
// This application will not start successfully until a Flyway migration script is created
// in `src/main/resources/db/migration` that defines the schema for the application's entities
// (e.g., City, Country, SystemParameter, etc.).
@SpringBootApplication(scanBasePackages = "com.lms")
@EnableCaching
public class SetupApplication {
  public static void main(String[] args) {
    SpringApplication.run(SetupApplication.class, args);
  }
}
