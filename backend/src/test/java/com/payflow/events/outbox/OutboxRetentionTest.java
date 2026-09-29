package com.payflow.events.outbox;

import com.payflow.events.EventMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRetentionTest {

    @Mock
    OutboxRepository repository;

    @Mock
    EventMetrics metrics;

    @Test
    void removesOnlyPublishedEventsOlderThanTheRetentionWindow() {
        var properties = new OutboxRelayProperties(
                "events", 20, Duration.ofSeconds(5), 5, Duration.ofDays(7), Duration.ofHours(1));
        when(repository.deletePublishedBefore(org.mockito.ArgumentMatchers.any())).thenReturn(3);

        new OutboxRetention(repository, properties, metrics).cleanPublished();

        verify(repository).deletePublishedBefore(argThat(
                threshold -> threshold.isBefore(Instant.now().minus(Duration.ofDays(7)).plusSeconds(1))));
        verify(metrics).outboxCleaned(3);
    }
}
