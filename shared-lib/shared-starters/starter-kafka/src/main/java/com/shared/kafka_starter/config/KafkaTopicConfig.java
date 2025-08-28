package com.shared.kafka_starter.config;


import com.shared.kafka_starter.props.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KafkaTopicConfig {

@Bean
@ConditionalOnProperty(prefix = "shared.kafka", name = "auto-create-topics", havingValue = "true")
public List<NewTopic> topics(KafkaProperties props) {
 List<NewTopic> list = new ArrayList<>();
 Map<String,String> map = props.getTopics();
 if (map != null) {
   map.forEach((t, pr) -> {
     String[] p = pr.split(",");
     list.add(new NewTopic(t, Integer.parseInt(p[0]), Short.parseShort(p[1])));
     list.add(new NewTopic(t + ".dlt", Integer.parseInt(p[0]), Short.parseShort(p[1])));
   });
 }
 return list;
}
}
