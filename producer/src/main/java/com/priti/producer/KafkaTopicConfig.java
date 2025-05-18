package com.priti.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic createMyTopic1() {
        return new NewTopic("my-new-topic-2", 3, (short) 1);//new topic name, partitions, replication factor
    }
}
