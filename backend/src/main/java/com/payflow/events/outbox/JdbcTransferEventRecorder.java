package com.payflow.events.outbox;

import com.payflow.transfers.Transfer;
import com.payflow.transfers.TransferEventRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class JdbcTransferEventRecorder implements TransferEventRecorder {

    static final String COMPLETED_EVENT_TYPE = "TransferCompleted";
    static final String REVERSED_EVENT_TYPE = "TransferReversed";
    static final int EVENT_VERSION = 1;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcTransferEventRecorder(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recordCompleted(Transfer transfer, String correlationId) {
        UUID eventId = UUID.randomUUID();
        UUID parsedCorrelationId = UUID.fromString(correlationId);
        Instant occurredAt = transfer.getCreatedAt();
        var event = new TransferCompletedV1(
                eventId,
                COMPLETED_EVENT_TYPE,
                EVENT_VERSION,
                occurredAt,
                parsedCorrelationId,
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount().toPlainString(),
                transfer.getCurrency()
        );

        persist(event);
    }

    @Override
    public void recordReversed(Transfer reversal, String correlationId) {
        UUID eventId = UUID.randomUUID();
        UUID parsedCorrelationId = UUID.fromString(correlationId);
        Instant occurredAt = reversal.getCreatedAt();
        var event = new TransferReversedV1(
                eventId,
                REVERSED_EVENT_TYPE,
                EVENT_VERSION,
                occurredAt,
                parsedCorrelationId,
                reversal.getId(),
                reversal.getOriginalTransferId(),
                reversal.getSourceAccountId(),
                reversal.getDestinationAccountId(),
                reversal.getAmount().toPlainString(),
                reversal.getCurrency()
        );
        persist(event);
    }

    private void persist(TransferEventV1 event) {
        jdbcTemplate.update("""
                INSERT INTO outbox_events (
                    event_id, aggregate_type, aggregate_id, event_type, event_version,
                    occurred_at, correlation_id, payload, created_at
                ) VALUES (?, 'Transfer', ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                """,
                event.eventId(),
                event.transferId(),
                event.eventType(),
                event.eventVersion(),
                Timestamp.from(event.occurredAt()),
                event.correlationId(),
                serialize(event),
                Timestamp.from(Instant.now())
        );
    }

    private String serialize(TransferEventV1 event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize transfer event.", exception);
        }
    }
}
