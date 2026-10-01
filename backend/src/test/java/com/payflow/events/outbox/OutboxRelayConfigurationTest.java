package com.payflow.events.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayConfigurationTest {

    @Test
    void createsTheSingleNodeTransferTopic() {
        var properties = new OutboxRelayProperties(
                "payflow.transfer-events.v1", 20, Duration.ofSeconds(5), 5,
                Duration.ofDays(7), Duration.ofHours(1));

        var topic = new OutboxRelayConfiguration().transferEventsTopic(properties);

        assertThat(topic.name()).isEqualTo("payflow.transfer-events.v1");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
        assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
    }
}
