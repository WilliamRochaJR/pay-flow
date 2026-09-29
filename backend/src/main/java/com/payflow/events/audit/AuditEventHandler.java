package com.payflow.events.audit;

import com.payflow.events.outbox.TransferCompletedV1;
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

    private static final String SUPPORTED_EVENT_TYPE = "TransferCompleted";
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
        TransferCompletedV1 event = deserialize(payload);
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

    private TransferCompletedV1 deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, TransferCompletedV1.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Invalid TransferCompleted.v1 payload.", exception);
        }
    }

    private void validateContract(TransferCompletedV1 event) {
        if (!SUPPORTED_EVENT_TYPE.equals(event.eventType()) || event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            throw new IllegalArgumentException("Unsupported transfer event contract.");
        }
    }
}
