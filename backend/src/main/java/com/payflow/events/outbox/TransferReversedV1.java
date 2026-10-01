package com.payflow.events.outbox;

import java.time.Instant;
import java.util.UUID;

public record TransferReversedV1(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        UUID transferId,
        UUID originalTransferId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        String amount,
        String currency
) implements TransferEventV1 {
}
