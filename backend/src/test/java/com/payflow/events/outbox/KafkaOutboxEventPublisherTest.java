package com.payflow.events.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxEventPublisherTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void waitsForKafkaToAcknowledgeTheEvent() {
        OutboxEvent event = event();
        CompletableFuture<SendResult<String, String>> acknowledged = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("events", event.aggregateId().toString(), event.payload()))
                .thenReturn(acknowledged);

        publisher(Duration.ofSeconds(1)).publish(event);

        verify(kafkaTemplate).send("events", event.aggregateId().toString(), event.payload());
    }

    @Test
    void reportsAProducerFailure() {
        OutboxEvent event = event();
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker rejected record"));
        when(kafkaTemplate.send("events", event.aggregateId().toString(), event.payload())).thenReturn(failed);

        assertThatThrownBy(() -> publisher(Duration.ofSeconds(1)).publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to publish an outbox event.");
    }

    @Test
    void reportsAnAcknowledgementTimeout() {
        OutboxEvent event = event();
        CompletableFuture<SendResult<String, String>> pending = new CompletableFuture<>();
        when(kafkaTemplate.send("events", event.aggregateId().toString(), event.payload())).thenReturn(pending);

        assertThatThrownBy(() -> publisher(Duration.ZERO).publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to publish an outbox event.");
    }

    @Test
    void restoresTheInterruptFlagWhenWaitingIsInterrupted() {
        OutboxEvent event = event();
        CompletableFuture<SendResult<String, String>> pending = new CompletableFuture<>();
        when(kafkaTemplate.send("events", event.aggregateId().toString(), event.payload())).thenReturn(pending);

        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> publisher(Duration.ofSeconds(1)).publish(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Interrupted while publishing an outbox event.");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private KafkaOutboxEventPublisher publisher(Duration timeout) {
        return new KafkaOutboxEventPublisher(
                kafkaTemplate,
                new OutboxRelayProperties("events", 20, timeout)
        );
    }

    private OutboxEvent event() {
        return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "{\"eventType\":\"TransferCompleted\"}");
    }
}
