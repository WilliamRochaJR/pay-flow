package com.payflow.events.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(name = "app.events.relay.enabled", havingValue = "true")
class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRelayProperties properties;

    KafkaOutboxEventPublisher(KafkaTemplate<String, String> kafkaTemplate, OutboxRelayProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(OutboxEvent event) {
        try {
            kafkaTemplate.send(properties.topic(), event.aggregateId().toString(), event.payload())
                    .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing an outbox event.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Unable to publish an outbox event.", exception);
        }
    }
}
