package com.payflow.events.audit;

import com.payflow.events.outbox.TransferCompletedV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
class AuditEventHandler {

    private static final String SUPPORTED_EVENT_TYPE = "TransferCompleted";
    private static final int SUPPORTED_EVENT_VERSION = 1;

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;
    private final String consumerName;

    AuditEventHandler(AuditEventRepository repository, ObjectMapper objectMapper,
                      @Value("${app.events.audit.consumer-name}") String consumerName) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.consumerName = consumerName;
    }

    @Transactional
    public void handle(String payload) {
        TransferCompletedV1 event = deserialize(payload);
        validateContract(event);
        if (!repository.claim(consumerName, event.eventId())) {
            return;
        }
        repository.record(event, payload);
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
