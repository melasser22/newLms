package com.lms.tenant.persistence.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@Configuration
@EnableJpaRepositories(basePackages = "com.lms.tenant.persistence.repo")
public class PersistenceAutoConfiguration { }
