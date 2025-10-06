package com.ejada.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * Entry point for the LMS API Gateway.
 */
@SpringBootApplication
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(ApiGatewayApplication.class);
    BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2048);
    applicationStartup.startRecording();
    application.setApplicationStartup(applicationStartup);
    application.run(args);
  }
}
