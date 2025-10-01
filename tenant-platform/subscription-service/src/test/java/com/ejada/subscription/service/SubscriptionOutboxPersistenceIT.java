package com.ejada.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.subscription.acl.MarketplaceCallbackOrchestrator;
import com.ejada.subscription.kafka.SubscriptionApprovalPublisher;
import com.ejada.subscription.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class SubscriptionOutboxPersistenceIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "subscription");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgresql");
        registry.add("spring.flyway.schemas", () -> "subscription");
        registry.add("spring.flyway.default-schema", () -> "subscription");
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    private MarketplaceCallbackOrchestrator orchestrator;

    @Autowired
    private OutboxEventRepository outboxRepo;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();
        orchestrator = new MarketplaceCallbackOrchestrator(
                Mockito.mock(com.ejada.subscription.repository.SubscriptionRepository.class),
                Mockito.mock(com.ejada.subscription.repository.SubscriptionFeatureRepository.class),
                Mockito.mock(com.ejada.subscription.repository.SubscriptionAdditionalServiceRepository.class),
                Mockito.mock(com.ejada.subscription.repository.SubscriptionProductPropertyRepository.class),
                Mockito.mock(com.ejada.subscription.repository.SubscriptionEnvironmentIdentifierRepository.class),
                Mockito.mock(com.ejada.subscription.repository.InboundNotificationAuditRepository.class),
                Mockito.mock(com.ejada.subscription.repository.SubscriptionUpdateEventRepository.class),
                outboxRepo,
                Mockito.mock(com.ejada.subscription.repository.IdempotentRequestRepository.class),
                Mockito.mock(com.ejada.subscription.mapper.SubscriptionMapper.class),
                Mockito.mock(com.ejada.subscription.mapper.SubscriptionFeatureMapper.class),
                Mockito.mock(com.ejada.subscription.mapper.SubscriptionAdditionalServiceMapper.class),
                Mockito.mock(com.ejada.subscription.mapper.SubscriptionProductPropertyMapper.class),
                Mockito.mock(com.ejada.subscription.mapper.SubscriptionEnvironmentIdentifierMapper.class),
                Mockito.mock(com.ejada.subscription.mapper.SubscriptionUpdateEventMapper.class),
                new ObjectMapper(),
                txManager,
                Mockito.mock(SubscriptionApprovalPublisher.class));
        outboxRepo.deleteAll();
    }

    @Test
    void emitOutboxPersistsEventsUsingTransactionalOutbox() {
        Map<String, Object> payload = Map.of("status", "CREATED", "rqUid", UUID.randomUUID().toString());

        ReflectionTestUtils.invokeMethod(
                orchestrator, "emitOutbox", "SUBSCRIPTION", "ext-123", "CREATED_OR_UPDATED", payload);

        List<com.ejada.subscription.model.OutboxEvent> events = outboxRepo.findAll();

        assertThat(events)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    assertThat(event.getAggregateType()).isEqualTo("SUBSCRIPTION");
                    assertThat(event.getAggregateId()).isEqualTo("ext-123");
                    assertThat(event.getEventType()).isEqualTo("CREATED_OR_UPDATED");
                    assertThat(event.getPayload()).contains("\"status\":\"CREATED\"");
                });
    }
}
