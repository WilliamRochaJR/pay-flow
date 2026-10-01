package com.payflow.events.outbox;

import com.payflow.events.EventMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxRepository repository;

    @Mock
    OutboxEventPublisher publisher;

    @Mock
    EventMetrics metrics;

    @Test
    void marksAnEventOnlyAfterPublishingIt() {
        OutboxEvent event = event();
        when(repository.lockPending(20, 5)).thenReturn(List.of(event));
        var relay = relay();

        relay.publishPending();

        InOrder order = inOrder(publisher, repository);
        order.verify(repository).markAttempt(event.eventId());
        order.verify(publisher).publish(event);
        order.verify(repository).markPublished(event.eventId());
        verify(metrics).relayPublished();
    }

    @Test
    void doesNothingWhenThereAreNoPendingEvents() {
        when(repository.lockPending(20, 5)).thenReturn(List.of());

        relay().publishPending();

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void keepsAnEventPendingWhenPublishingFails() {
        OutboxEvent event = event();
        when(repository.lockPending(20, 5)).thenReturn(List.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("Kafka unavailable"))
                .when(publisher).publish(event);

        relay().publishPending();

        verify(repository, never()).markPublished(event.eventId());
        verify(repository).markFailed(event.eventId(), "Kafka unavailable", false);
        verify(metrics).relayFailed(false);
    }

    @Test
    void exhaustsAnEventOnItsLastAttempt() {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "payload", 4);
        when(repository.lockPending(20, 5)).thenReturn(List.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("Kafka unavailable"))
                .when(publisher).publish(event);

        relay().publishPending();

        verify(repository).markFailed(event.eventId(), "Kafka unavailable", true);
        verify(metrics).relayFailed(true);
    }

    private OutboxRelay relay() {
        return new OutboxRelay(
                repository,
                publisher,
                properties(),
                metrics
        );
    }

    private OutboxEvent event() {
        return new OutboxEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "{\"eventType\":\"TransferCompleted\"}", 0);
    }

    private OutboxRelayProperties properties() {
        return new OutboxRelayProperties(
                "payflow.transfer-events.v1", 20, Duration.ofSeconds(5), 5,
                Duration.ofDays(7), Duration.ofHours(1));
    }
}
