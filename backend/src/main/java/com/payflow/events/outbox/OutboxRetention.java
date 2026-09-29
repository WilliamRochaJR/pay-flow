package com.payflow.events.outbox;

import com.payflow.events.EventMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.events.relay.enabled", havingValue = "true")
class OutboxRetention {

    private final OutboxRepository repository;
    private final OutboxRelayProperties properties;
    private final EventMetrics metrics;

    OutboxRetention(OutboxRepository repository, OutboxRelayProperties properties, EventMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.events.relay.cleanup-delay:1h}")
    @Transactional
    public void cleanPublished() {
        int deleted = repository.deletePublishedBefore(Instant.now().minus(properties.retention()));
        metrics.outboxCleaned(deleted);
    }
}
