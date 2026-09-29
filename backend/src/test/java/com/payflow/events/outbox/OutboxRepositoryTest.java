package com.payflow.events.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    ResultSet resultSet;

    @Test
    void mapsAndLocksPendingEvents() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        when(resultSet.getObject("event_id", UUID.class)).thenReturn(eventId);
        when(resultSet.getObject("aggregate_id", UUID.class)).thenReturn(aggregateId);
        when(resultSet.getString("payload")).thenReturn("{\"eventVersion\":1}");
        when(jdbcTemplate.query(
                anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<OutboxEvent>>any(),
                eq(20)
        )).thenAnswer(invocation -> {
            RowMapper<OutboxEvent> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });

        List<OutboxEvent> events = new OutboxRepository(jdbcTemplate).lockPending(20);

        assertThat(events).containsExactly(new OutboxEvent(eventId, aggregateId, "{\"eventVersion\":1}"));
    }

    @Test
    void marksOnlyTheRequestedPendingEventAsPublished() {
        UUID eventId = UUID.randomUUID();

        new OutboxRepository(jdbcTemplate).markPublished(eventId);

        verify(jdbcTemplate).update(anyString(), eq(eventId));
    }
}
