package com.ejada.sec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the security service.  This class bootstraps the Spring Boot
 * application and performs component scanning on the {@code com.ejada.secservice}
 * package.  The service builds on top of the shared libraries provided in the
 * parent repository and exposes authentication and user management APIs.
 */
@SpringBootApplication
public final class SecServiceApplication {

    private SecServiceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(SecServiceApplication.class, args);
    }
}