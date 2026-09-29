package com.payflow.events.audit;

import com.payflow.events.outbox.TransferCompletedV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventRepositoryTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    void claimsAnEventWhenTheDeduplicationRowIsInserted() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq("payflow-audit"), eq(eventId), any())).thenReturn(1);

        boolean claimed = new AuditEventRepository(jdbcTemplate).claim("payflow-audit", eventId);

        assertThat(claimed).isTrue();
    }

    @Test
    void reportsADuplicateWhenNoDeduplicationRowIsInserted() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq("payflow-audit"), eq(eventId), any())).thenReturn(0);

        boolean claimed = new AuditEventRepository(jdbcTemplate).claim("payflow-audit", eventId);

        assertThat(claimed).isFalse();
    }

    @Test
    void storesTheOriginalPayloadWithItsSearchableIdentifiers() {
        TransferCompletedV1 event = event();

        new AuditEventRepository(jdbcTemplate).record(event, "payload");

        verify(jdbcTemplate).update(
                anyString(),
                eq(event.eventId()),
                eq(event.transferId()),
                eq(event.eventType()),
                eq(event.eventVersion()),
                any(),
                eq(event.correlationId()),
                eq("payload"),
                any()
        );
    }

    private TransferCompletedV1 event() {
        return new TransferCompletedV1(
                UUID.randomUUID(),
                "TransferCompleted",
                1,
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
