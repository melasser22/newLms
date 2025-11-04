package com.ejada.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the security service.  This class bootstraps the Spring Boot
 * application and performs component scanning on the {@code com.ejada.secservice}
 * package.  The service builds on top of the shared libraries provided in the
 * parent repository and exposes authentication and user management APIs.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class AdminServiceApplication {

    private AdminServiceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}