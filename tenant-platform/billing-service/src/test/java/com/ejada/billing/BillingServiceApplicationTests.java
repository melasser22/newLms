package com.ejada.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.ejada.billing.repository.TenantOverageRepository;

/**
 * Verifies that the Spring application context can start without requiring
 * external infrastructure such as the PostgreSQL database. The various data
 * source related auto configurations are disabled and the JPA repository is
 * mocked so the context loads in isolation.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class BillingServiceApplicationTests {

    /**
     * Mock the JPA repository so that components depending on it can be created
     * without an actual database.
     */
    @MockBean
    TenantOverageRepository repository;

    @Test
    void contextLoads() {
        // If the application context fails to start, this test will fail.
    }
}
