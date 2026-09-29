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

    static final String EVENT_TYPE = "TransferCompleted";
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
                EVENT_TYPE,
                EVENT_VERSION,
                occurredAt,
                parsedCorrelationId,
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount().toPlainString(),
                transfer.getCurrency()
        );

        jdbcTemplate.update("""
                INSERT INTO outbox_events (
                    event_id, aggregate_type, aggregate_id, event_type, event_version,
                    occurred_at, correlation_id, payload, created_at
                ) VALUES (?, 'Transfer', ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                """,
                eventId,
                transfer.getId(),
                EVENT_TYPE,
                EVENT_VERSION,
                Timestamp.from(occurredAt),
                parsedCorrelationId,
                serialize(event),
                Timestamp.from(Instant.now())
        );
    }

    private String serialize(TransferCompletedV1 event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize TransferCompleted.v1.", exception);
        }
    }
}
