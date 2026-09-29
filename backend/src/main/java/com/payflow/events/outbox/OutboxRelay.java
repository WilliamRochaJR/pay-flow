package com.payflow.events.outbox;

import com.payflow.events.EventMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.events.relay.enabled", havingValue = "true")
class OutboxRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final OutboxEventPublisher publisher;
    private final OutboxRelayProperties properties;
    private final EventMetrics metrics;

    OutboxRelay(OutboxRepository repository, OutboxEventPublisher publisher, OutboxRelayProperties properties,
                EventMetrics metrics) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.events.relay.fixed-delay:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.lockPending(properties.batchSize(), properties.maxAttempts())) {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", event.correlationId().toString())) {
                repository.markAttempt(event.eventId());
                try {
                    publisher.publish(event);
                    repository.markPublished(event.eventId());
                    metrics.relayPublished();
                } catch (RuntimeException exception) {
                    boolean exhausted = event.attempts() + 1 >= properties.maxAttempts();
                    repository.markFailed(event.eventId(), safeMessage(exception), exhausted);
                    metrics.relayFailed(exhausted);
                    LOGGER.warn("outbox_publish_failed eventId={} attempt={} exhausted={}",
                            event.eventId(), event.attempts() + 1, exhausted);
                }
            }
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 500));
    }
}
