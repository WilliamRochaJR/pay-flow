package com.payflow.events.audit;

import com.payflow.events.EventMetrics;
import com.payflow.events.outbox.TransferCompletedV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventHandlerTest {

    @Mock
    AuditEventRepository repository;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    EventMetrics metrics;

    @Test
    void recordsAnEventClaimedForTheFirstTime() throws Exception {
        TransferCompletedV1 event = event("TransferCompleted", 1);
        when(objectMapper.readValue("payload", TransferCompletedV1.class)).thenReturn(event);
        when(repository.claim("payflow-audit", event.eventId())).thenReturn(true);

        handler().handle("payload");

        verify(repository).record(event, "payload");
        verify(metrics).auditProcessed(false);
    }

    @Test
    void ignoresAnEventAlreadyProcessedByThisConsumer() throws Exception {
        TransferCompletedV1 event = event("TransferCompleted", 1);
        when(objectMapper.readValue("payload", TransferCompletedV1.class)).thenReturn(event);
        when(repository.claim("payflow-audit", event.eventId())).thenReturn(false);

        handler().handle("payload");

        verify(repository, never()).record(event, "payload");
        verify(metrics).auditProcessed(true);
    }

    @Test
    void rejectsAnUnsupportedContractBeforeClaimingIt() throws Exception {
        TransferCompletedV1 event = event("TransferCompleted", 2);
        when(objectMapper.readValue("payload", TransferCompletedV1.class)).thenReturn(event);

        assertThatThrownBy(() -> handler().handle("payload"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported transfer event contract.");
        verify(repository, never()).claim("payflow-audit", event.eventId());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        JacksonException failure = org.mockito.Mockito.mock(JacksonException.class);
        when(objectMapper.readValue("invalid", TransferCompletedV1.class)).thenThrow(failure);

        assertThatThrownBy(() -> handler().handle("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid TransferCompleted.v1 payload.")
                .hasCause(failure);
    }

    private AuditEventHandler handler() {
        return new AuditEventHandler(
                repository,
                objectMapper,
                new EventAuditProperties(
                        "payflow-audit", "payflow-audit-v1", "events.DLT",
                        java.time.Duration.ofSeconds(1), 2, java.time.Duration.ofDays(7)),
                metrics
        );
    }

    private TransferCompletedV1 event(String type, int version) {
        return new TransferCompletedV1(
                UUID.randomUUID(),
                type,
                version,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "25.00",
                "BRL"
        );
    }
}
