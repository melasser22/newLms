package com.ejada.management;

import com.ejada.management.config.ChildServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(ChildServiceProperties.class)
public class EmailManagementServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailManagementServiceApplication.class, args);
  }
}
