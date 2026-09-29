package com.payflow.events.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void marksAnEventOnlyAfterPublishingIt() {
        OutboxEvent event = event();
        when(repository.lockPending(20)).thenReturn(List.of(event));
        var relay = relay();

        relay.publishPending();

        InOrder order = inOrder(publisher, repository);
        order.verify(publisher).publish(event);
        order.verify(repository).markPublished(event.eventId());
    }

    @Test
    void doesNothingWhenThereAreNoPendingEvents() {
        when(repository.lockPending(20)).thenReturn(List.of());

        relay().publishPending();

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void keepsAnEventPendingWhenPublishingFails() {
        OutboxEvent event = event();
        when(repository.lockPending(20)).thenReturn(List.of(event));
        org.mockito.Mockito.doThrow(new IllegalStateException("Kafka unavailable"))
                .when(publisher).publish(event);

        assertThatThrownBy(() -> relay().publishPending())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Kafka unavailable");
        verify(repository, never()).markPublished(event.eventId());
    }

    private OutboxRelay relay() {
        return new OutboxRelay(
                repository,
                publisher,
                new OutboxRelayProperties("payflow.transfer-completed.v1", 20, Duration.ofSeconds(5))
        );
    }

    private OutboxEvent event() {
        return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "{\"eventType\":\"TransferCompleted\"}");
    }
}
