package com.ejada.kafka_starter.config;

import com.ejada.kafka_starter.props.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(KafkaAdmin.class)
public class KafkaTopicConfig {

    @Bean
    @ConditionalOnProperty(prefix = "shared.kafka", name = "auto-create-topics", havingValue = "true")
    public List<NewTopic> topics(final KafkaProperties props) {
        Map<String, String> map = props.getTopics();
        List<NewTopic> topics = new ArrayList<>();
        if (map == null || map.isEmpty()) {
            return topics;
        }

        map.forEach((topicName, partitionReplica) -> {
            String[] parts = partitionReplica.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid topic definition for " + topicName
                        + ": expected format 'partitions,replicas'");
            }
            int partitions = Integer.parseInt(parts[0].trim());
            short replicas = Short.parseShort(parts[1].trim());

            topics.add(new NewTopic(topicName, partitions, replicas));
            topics.add(new NewTopic(topicName + ".dlt", partitions, replicas));
        });

        return topics;
    }
}
