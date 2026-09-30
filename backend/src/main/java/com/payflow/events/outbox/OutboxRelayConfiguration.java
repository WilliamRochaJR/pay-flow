package com.payflow.events.outbox;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(OutboxRelayProperties.class)
@ConditionalOnProperty(name = "app.events.relay.enabled", havingValue = "true")
class OutboxRelayConfiguration {

    @Bean
    NewTopic transferEventsTopic(OutboxRelayProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, Long.toString(properties.retention().toMillis()))
                .build();
    }
}
