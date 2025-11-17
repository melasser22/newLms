package com.ejada.push.management;

import com.ejada.push.management.config.ChildServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ChildServiceProperties.class)
public class PushManagementServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PushManagementServiceApplication.class, args);
  }
}
