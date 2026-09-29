package com.payflow.events.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Repository
class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    OutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<OutboxEvent> lockPending(int batchSize, int maxAttempts) {
        return jdbcTemplate.query("""
                SELECT event_id, aggregate_id, correlation_id, payload::text, attempts
                FROM outbox_events
                WHERE published_at IS NULL
                  AND exhausted_at IS NULL
                  AND attempts < ?
                ORDER BY created_at, event_id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """, (resultSet, rowNumber) -> new OutboxEvent(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("aggregate_id", UUID.class),
                resultSet.getObject("correlation_id", UUID.class),
                resultSet.getString("payload"),
                resultSet.getInt("attempts")
        ), maxAttempts, batchSize);
    }

    void markAttempt(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET attempts = attempts + 1, last_attempt_at = NOW(), last_error = NULL
                WHERE event_id = ? AND published_at IS NULL
                """, eventId);
    }

    void markPublished(UUID eventId) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET published_at = NOW(), last_error = NULL
                WHERE event_id = ? AND published_at IS NULL
                """, eventId);
    }

    void markFailed(UUID eventId, String error, boolean exhausted) {
        jdbcTemplate.update("""
                UPDATE outbox_events
                SET last_error = ?, exhausted_at = CASE WHEN ? THEN NOW() ELSE exhausted_at END
                WHERE event_id = ? AND published_at IS NULL
                """, error, exhausted, eventId);
    }

    int deletePublishedBefore(Instant threshold) {
        return jdbcTemplate.update("""
                DELETE FROM outbox_events
                WHERE published_at IS NOT NULL AND published_at < ?
                """, threshold);
    }
}
