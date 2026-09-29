package com.payflow.events.audit;

import com.payflow.events.EventMetrics;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EventAuditProperties.class)
@ConditionalOnProperty(name = "app.events.audit.enabled", havingValue = "true")
class EventAuditConfiguration {

    @Bean
    org.apache.kafka.clients.admin.NewTopic auditDeadLetterTopic(EventAuditProperties properties) {
        return TopicBuilder.name(properties.deadLetterTopic())
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, Long.toString(properties.retention().toMillis()))
                .build();
    }

    @Bean
    DefaultErrorHandler auditErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            EventAuditProperties properties,
            EventMetrics metrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(properties.deadLetterTopic(), record.partition())
        );
        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(properties.retryInterval().toMillis(), properties.retryAttempts())
        );
        handler.setRetryListeners(new AuditRetryListener(properties.retryAttempts(), metrics));
        return handler;
    }
}
