package com.payflow.events.outbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayConfigurationTest {

    @Test
    void createsTheSingleNodeTransferTopic() {
        var properties = new OutboxRelayProperties("payflow.transfer-completed.v1", 20, Duration.ofSeconds(5));

        var topic = new OutboxRelayConfiguration().transferCompletedTopic(properties);

        assertThat(topic.name()).isEqualTo("payflow.transfer-completed.v1");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }
}
