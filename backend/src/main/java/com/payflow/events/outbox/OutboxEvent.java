package com.payflow.events.outbox;

import java.util.UUID;

record OutboxEvent(UUID eventId, UUID aggregateId, String payload) {
}
