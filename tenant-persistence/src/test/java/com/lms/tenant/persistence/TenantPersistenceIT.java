package com.lms.tenant.persistence;

import com.lms.tenant.persistence.entity.Tenant;
import com.lms.tenant.persistence.repo.TenantRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TenantPersistenceIT {
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

  @SpringBootApplication(scanBasePackages = "com.lms.tenant.persistence")
  static class TestApp { }

  @BeforeAll static void init() { pg.start(); System.setProperty("spring.datasource.url", pg.getJdbcUrl()); System.setProperty("spring.datasource.username", pg.getUsername()); System.setProperty("spring.datasource.password", pg.getPassword()); System.setProperty("spring.flyway.locations", "classpath:db/migration"); }
  @AfterAll static void stop() { pg.stop(); }

  @Autowired TenantRepository repo;

  @Test
  void canPersistTenant() {
    Tenant t = new Tenant();
    t.setSlug("it-tenant");
    t.setName("IT");
    t = repo.save(t);
    assertThat(t.getId()).isNotNull();
    assertThat(repo.findBySlug("it-tenant")).isPresent();
  }
}
