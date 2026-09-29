package com.payflow.events.audit;

import com.payflow.events.EventMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditRetryListenerTest {

    @Mock
    EventMetrics metrics;

    @Test
    void countsOnlyTheConfiguredRetriesAndTheDeadLetterRecovery() {
        AuditRetryListener listener = new AuditRetryListener(2, metrics);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("events", 0, 0, "key", "payload");
        Exception failure = new IllegalArgumentException("invalid");

        listener.failedDelivery(record, failure, 1);
        listener.failedDelivery(record, failure, 2);
        listener.failedDelivery(record, failure, 3);
        listener.recovered(record, failure);

        verify(metrics, org.mockito.Mockito.times(2)).auditRetry();
        verify(metrics).auditDeadLettered();
        verify(metrics, never()).relayPublished();
    }
}
