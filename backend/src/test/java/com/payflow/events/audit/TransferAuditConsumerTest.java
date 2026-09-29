package com.payflow.events.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferAuditConsumerTest {

    @Mock
    AuditEventHandler handler;

    @Test
    void delegatesTheReceivedPayloadToTheTransactionalHandler() {
        new TransferAuditConsumer(handler).consume("payload");

        verify(handler).handle("payload");
    }
}
