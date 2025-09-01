package com.lms.tenant.events;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/** Polls the outbox table and publishes tenant events. */
public class TenantEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(TenantEventPublisher.class);

    private final JdbcTemplate jdbc;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaProvider;
    private final TenantEventsProperties properties;

    public TenantEventPublisher(JdbcTemplate jdbc,
                                ObjectProvider<KafkaTemplate<String, String>> kafkaProvider,
                                TenantEventsProperties properties) {
        this.jdbc = jdbc;
        this.kafkaProvider = kafkaProvider;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@tenantEventsProperties.pollInterval.toMillis()}")
    @Transactional
    public void publish() {
        List<OutboxRow> rows = jdbc.query(
            "SELECT id, tenant_id, type, payload FROM tenant_outbox WHERE status='NEW' AND available_at <= now() ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED",
            new OutboxRowMapper(), properties.getBatchSize());
        KafkaTemplate<String, String> kafka = kafkaProvider.getIfAvailable();
        for (OutboxRow row : rows) {
            boolean success = false;
            try {
                if (kafka != null) {
                    ProducerRecord<String, String> record = new ProducerRecord<>(row.type(), row.payload());
                    record.headers().add("X-Tenant-Id", row.tenantId().toString().getBytes(StandardCharsets.UTF_8));
                    kafka.send(record);
                } else {
                    log.info("Tenant event {}: {}", row.type(), row.payload());
                }
                success = true;
            } catch (Exception ex) {
                log.warn("Failed to publish tenant event {}", row.id(), ex);
            }
            if (success) {
                jdbc.update("UPDATE tenant_outbox SET status='SENT', attempts = attempts + 1 WHERE id = ?", row.id());
            } else {
                jdbc.update("UPDATE tenant_outbox SET attempts = attempts + 1 WHERE id = ?", row.id());
            }
        }
    }

    private record OutboxRow(UUID id, UUID tenantId, String type, String payload) {}

    private static class OutboxRowMapper implements RowMapper<OutboxRow> {
        @Override
        public OutboxRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OutboxRow(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("type"),
                rs.getString("payload"));
        }
    }
}
