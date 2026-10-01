package com.payflow.events.outbox;

import java.time.Instant;
import java.util.UUID;

public interface TransferEventV1 {

    UUID eventId();

    String eventType();

    int eventVersion();

    Instant occurredAt();

    UUID correlationId();

    UUID transferId();
}
