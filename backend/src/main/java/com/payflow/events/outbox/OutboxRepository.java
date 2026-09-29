package com.payflow.events.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    OutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<OutboxEvent> lockPending(int batchSize) {
        return jdbcTemplate.query("""
                SELECT event_id, aggregate_id, payload::text
                FROM outbox_events
                WHERE published_at IS NULL
                ORDER BY created_at, event_id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """, (resultSet, rowNumber) -> new OutboxEvent(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("aggregate_id", UUID.class),
                resultSet.getString("payload")
        ), batchSize);
    }

    void markPublished(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET published_at = NOW()
                WHERE event_id = ? AND published_at IS NULL
                """, eventId);
    }
}
