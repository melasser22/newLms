package com.lms.tenant.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = TenantEventsAutoConfiguration.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.task.scheduling.enabled=false"
})
class TenantEventPublisherTests {

    @Autowired
    TenantEventWriter writer;

    @Autowired
    TenantEventPublisher publisher;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void insertAndPublishWithoutKafkaMarksSent() {
        UUID tenantId = UUID.randomUUID();
        writer.write(tenantId, "test-topic", Map.of("hello", "world"));

        publisher.publish();

        String status = jdbc.queryForObject("select status from tenant_outbox", String.class);
        Integer attempts = jdbc.queryForObject("select attempts from tenant_outbox", Integer.class);
        assertThat(status).isEqualTo("SENT");
        assertThat(attempts).isEqualTo(1);
    }
}
