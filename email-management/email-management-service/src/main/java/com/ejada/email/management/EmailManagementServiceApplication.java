package com.ejada.email.management;

import com.ejada.email.management.config.ChildServiceProperties;
import com.ejada.email.management.config.GlobalConfigProperties;
import com.ejada.email.management.config.TenantSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties({
  ChildServiceProperties.class,
  TenantSecurityProperties.class,
  GlobalConfigProperties.class
})
public class EmailManagementServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailManagementServiceApplication.class, args);
  }
}
