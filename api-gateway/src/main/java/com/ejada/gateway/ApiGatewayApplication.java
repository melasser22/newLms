package com.ejada.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for the Ejada SaaS Products Framework API Gateway.
 */
@SpringBootApplication
public class ApiGatewayApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayApplication.class);

  public static void main(String[] args) {
    LOGGER.info("Starting API Gateway application...");
    SpringApplication application = new SpringApplication(ApiGatewayApplication.class);
    BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2048);
    applicationStartup.startRecording();
    application.setApplicationStartup(applicationStartup);
    application.run(args);
  }

  @Bean
  public ApplicationListener<ApplicationReadyEvent> applicationReadyListener() {
    return event -> {
      LOGGER.info("API Gateway application is ready and accepting requests");
      LOGGER.info("Active profiles: {}", String.join(", ", event.getApplicationContext().getEnvironment().getActiveProfiles()));
      LOGGER.info("Server port: {}", event.getApplicationContext().getEnvironment().getProperty("server.port", "8000"));
    };
  }
}
