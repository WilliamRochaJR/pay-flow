package com.payflow.events.audit;

import com.payflow.events.EventMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EventAuditConfigurationTest {

    @Mock
    KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    EventMetrics metrics;

    @Test
    void createsTheDeadLetterTopicWithRetention() {
        EventAuditProperties properties = properties();

        var topic = new EventAuditConfiguration().auditDeadLetterTopic(properties);

        assertThat(topic.name()).isEqualTo("events.DLT");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
        assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
    }

    @Test
    void createsTheLimitedRetryErrorHandler() {
        var handler = new EventAuditConfiguration().auditErrorHandler(kafkaTemplate, properties(), metrics);

        assertThat(handler).isNotNull();
    }

    private EventAuditProperties properties() {
        return new EventAuditProperties(
                "payflow-audit", "payflow-audit-v1", "events.DLT",
                Duration.ofSeconds(1), 2, Duration.ofDays(7));
    }
}
