package com.payflow.events.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.events.relay.enabled", havingValue = "true")
class OutboxRelay {

    private final OutboxRepository repository;
    private final OutboxEventPublisher publisher;
    private final OutboxRelayProperties properties;

    OutboxRelay(OutboxRepository repository, OutboxEventPublisher publisher, OutboxRelayProperties properties) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.events.relay.fixed-delay:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.lockPending(properties.batchSize())) {
            publisher.publish(event);
            repository.markPublished(event.eventId());
        }
    }
}
