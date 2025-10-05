package com.ejada.subscription.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.ejada.common.events.provisioning.ProvisionedAddon;
import com.ejada.common.events.provisioning.ProvisionedFeature;
import com.ejada.common.events.subscription.SubscriptionApprovalAction;
import com.ejada.common.events.subscription.SubscriptionApprovalMessage;
import com.ejada.subscription.SubscriptionApplication;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = SubscriptionApplication.class)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class SubscriptionApprovalConsumerIT {

    static final String APPROVAL_TOPIC = "subscription-approvals-it";
    private static final String CONSUMER_GROUP = "subscription-approvals-it-group";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"));

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
                    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "subscription");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add(
                "spring.flyway.locations",
                () -> "classpath:db/migration/postgresql,classpath:db/testdata/subscription");
        registry.add("spring.flyway.schemas", () -> "subscription");
        registry.add("spring.flyway.default-schema", () -> "subscription");
        registry.add("spring.flyway.create-schemas", () -> "true");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("shared.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add(
                "spring.kafka.producer.properties.spring.json.add.type.headers",
                () -> "false");
        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add(
                "spring.kafka.consumer.properties.spring.json.trusted.packages",
                () -> "*");
        registry.add(
                "spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "java.util.LinkedHashMap");
        registry.add("app.subscription-approval.topic", () -> APPROVAL_TOPIC);
        registry.add("app.subscription-approval.consumer-group", () -> CONSUMER_GROUP);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private TenantProvisioningPublisher provisioningPublisher;

    @Test
    void approvedMessageTriggersProvisioningPublish() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
                    latch.countDown();
                    return null;
                })
                .when(provisioningPublisher)
                .publish(any(), any(), any());

        UUID requestId = UUID.randomUUID();
        SubscriptionApprovalMessage message = new SubscriptionApprovalMessage(
                SubscriptionApprovalAction.APPROVED,
                requestId,
                9000L,
                7000L,
                "Ejada Corp",
                "إجادة",
                "admin@example.com",
                "+966500000000",
                "TEN-9000",
                "Ejada Corp",
                "ops@example.com",
                "+966500000000",
                "ejada-officer",
                OffsetDateTime.now(),
                null);

        kafkaTemplate
                .executeInTransaction(operations -> {
                    operations
                            .send(APPROVAL_TOPIC, message.requestId().toString(), message)
                            .get(10, TimeUnit.SECONDS);
                    return null;
                });

        assertThat(latch.await(15, TimeUnit.SECONDS)).as("listener should publish provisioning payload").isTrue();

        ArgumentCaptor<SubscriptionApprovalMessage> messageCaptor = ArgumentCaptor.forClass(SubscriptionApprovalMessage.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProvisionedFeature>> featuresCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProvisionedAddon>> addonsCaptor = ArgumentCaptor.forClass(List.class);

        verify(provisioningPublisher)
                .publish(messageCaptor.capture(), featuresCaptor.capture(), addonsCaptor.capture());

        SubscriptionApprovalMessage consumed = messageCaptor.getValue();
        assertThat(consumed.subscriptionId()).isEqualTo(9000L);
        List<ProvisionedFeature> features = featuresCaptor.getValue();
        assertThat(features)
                .hasSize(1)
                .first()
                .satisfies(feature -> {
                    assertThat(feature.code()).isEqualTo("API_CALLS");
                    assertThat(feature.quantity()).isEqualTo(25);
                });

        List<ProvisionedAddon> addons = addonsCaptor.getValue();
        assertThat(addons)
                .hasSize(1)
                .first()
                .satisfies(addon -> {
                    assertThat(addon.code()).isEqualTo("LOGS");
                    assertThat(addon.productAdditionalServiceId()).isEqualTo(9901L);
                    assertThat(addon.requestedCount()).isEqualTo(5L);
                });
    }
}
