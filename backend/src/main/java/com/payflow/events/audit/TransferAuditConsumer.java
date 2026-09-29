package com.payflow.events.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.events.audit.enabled", havingValue = "true")
class TransferAuditConsumer {

    private final AuditEventHandler handler;

    TransferAuditConsumer(AuditEventHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${app.events.relay.topic}",
            groupId = "${app.events.audit.consumer-group}"
    )
    void consume(String payload) {
        handler.handle(payload);
    }
}
