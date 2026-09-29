package com.payflow.events.audit;

import com.payflow.events.outbox.TransferCompletedV1;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
class AuditEventRepository {

    private final JdbcTemplate jdbcTemplate;

    AuditEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean claim(String consumerName, UUID eventId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_events (consumer_name, event_id, processed_at)
                VALUES (?, ?, ?)
                ON CONFLICT (consumer_name, event_id) DO NOTHING
                """, consumerName, eventId, Timestamp.from(Instant.now()));
        return inserted == 1;
    }

    void record(TransferCompletedV1 event, String payload) {
        jdbcTemplate.update("""
                INSERT INTO audit_events (
                    event_id, transfer_id, event_type, event_version,
                    occurred_at, correlation_id, payload, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                """,
                event.eventId(),
                event.transferId(),
                event.eventType(),
                event.eventVersion(),
                Timestamp.from(event.occurredAt()),
                event.correlationId(),
                payload,
                Timestamp.from(Instant.now())
        );
    }
}
