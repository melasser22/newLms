package com.ejada.tenant.kafka;

import com.ejada.common.events.subscription.SubscriptionApprovalProperties;
import com.ejada.kafka_starter.props.KafkaProperties;
import com.ejada.tenant.exception.TenantConflictException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Kafka listener infrastructure that provides retry and dead-letter handling for subscription
 * approvals.
 */
@Configuration
public class SubscriptionApprovalKafkaConfiguration {

    @Bean
    public DefaultErrorHandler subscriptionApprovalErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaProperties kafkaProperties,
            SubscriptionApprovalProperties approvalProperties) {

        var backoff = new ExponentialBackOffWithMaxRetries(kafkaProperties.getMaxAttempts() - 1);
        backoff.setInitialInterval(kafkaProperties.getBackoff().toMillis());
        backoff.setMultiplier(2.0d);
        backoff.setMaxInterval(10_000L);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) ->
                                new TopicPartition(
                                        approvalProperties.getTopic() + ".dlt",
                                        record.partition()));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backoff);
        errorHandler.addNotRetryableExceptions(
                TenantConflictException.class, IllegalArgumentException.class);
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            subscriptionApprovalListenerContainerFactory(
                    ConsumerFactory<String, Object> consumerFactory,
                    KafkaProperties kafkaProperties,
                    DefaultErrorHandler subscriptionApprovalErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(kafkaProperties.getConcurrency());
        factory.setCommonErrorHandler(subscriptionApprovalErrorHandler);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
