package com.payflow.events;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventMetricsTest {

    @Test
    void recordsEveryBoundedOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        EventMetrics metrics = new EventMetrics(registry);

        metrics.relayPublished();
        metrics.relayFailed(false);
        metrics.relayFailed(true);
        metrics.auditProcessed(false);
        metrics.auditProcessed(true);
        metrics.auditRetry();
        metrics.auditDeadLettered();
        metrics.outboxCleaned(4);

        assertThat(registry.get("payflow.events.relay").tag("outcome", "published").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.relay").tag("outcome", "retry").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.relay").tag("outcome", "exhausted").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.audit").tag("outcome", "processed").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.audit").tag("outcome", "duplicate").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.audit").tag("outcome", "retry").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.audit").tag("outcome", "dead-lettered").counter().count())
                .isEqualTo(1);
        assertThat(registry.get("payflow.events.outbox.cleaned").counter().count()).isEqualTo(4);
    }
}
