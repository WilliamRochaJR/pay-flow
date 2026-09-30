package com.payflow.events.audit;

import com.payflow.events.outbox.TransferCompletedV1;
import com.payflow.events.outbox.TransferEventV1;
import com.payflow.events.outbox.TransferReversedV1;
import com.payflow.events.EventMetrics;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "app.events.audit.enabled", havingValue = "true")
class AuditEventHandler {

    private static final String COMPLETED_EVENT_TYPE = "TransferCompleted";
    private static final String REVERSED_EVENT_TYPE = "TransferReversed";
    private static final int SUPPORTED_EVENT_VERSION = 1;

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;
    private final String consumerName;
    private final EventMetrics metrics;

    AuditEventHandler(AuditEventRepository repository, ObjectMapper objectMapper,
                      EventAuditProperties properties, EventMetrics metrics) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.consumerName = properties.consumerName();
        this.metrics = metrics;
    }

    @Transactional
    public void handle(String payload) {
        TransferEventV1 event = deserialize(payload);
        validateContract(event);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", event.correlationId().toString())) {
            if (!repository.claim(consumerName, event.eventId())) {
                metrics.auditProcessed(true);
                return;
            }
            repository.record(event, payload);
            metrics.auditProcessed(false);
        }
    }

    private TransferEventV1 deserialize(String payload) {
        try {
            EventDescriptor descriptor = objectMapper.readValue(payload, EventDescriptor.class);
            validateDescriptor(descriptor);
            return switch (descriptor.eventType()) {
                case COMPLETED_EVENT_TYPE -> objectMapper.readValue(payload, TransferCompletedV1.class);
                case REVERSED_EVENT_TYPE -> objectMapper.readValue(payload, TransferReversedV1.class);
                default -> throw new IllegalArgumentException("Unsupported transfer event contract.");
            };
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid transfer event payload.", exception);
        }
    }

    private void validateDescriptor(EventDescriptor descriptor) {
        if (descriptor == null
                || descriptor.eventVersion() != SUPPORTED_EVENT_VERSION
                || (!COMPLETED_EVENT_TYPE.equals(descriptor.eventType())
                && !REVERSED_EVENT_TYPE.equals(descriptor.eventType()))) {
            throw new IllegalArgumentException("Unsupported transfer event contract.");
        }
    }

    private void validateContract(TransferEventV1 event) {
        if (event.eventId() == null || event.occurredAt() == null || event.correlationId() == null
                || event.transferId() == null) {
            throw new IllegalArgumentException("Invalid transfer event contract.");
        }
        if (event instanceof TransferCompletedV1 completed
                && (completed.sourceAccountId() == null || completed.destinationAccountId() == null
                || completed.amount() == null || completed.currency() == null)) {
            throw new IllegalArgumentException("Invalid transfer event contract.");
        }
        if (event instanceof TransferReversedV1 reversed
                && (reversed.originalTransferId() == null || reversed.sourceAccountId() == null
                || reversed.destinationAccountId() == null || reversed.amount() == null
                || reversed.currency() == null)) {
            throw new IllegalArgumentException("Invalid transfer event contract.");
        }
    }

    public record EventDescriptor(String eventType, int eventVersion) {
    }
}
