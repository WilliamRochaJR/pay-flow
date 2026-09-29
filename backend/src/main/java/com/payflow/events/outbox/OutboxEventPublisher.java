package com.payflow.events.outbox;

interface OutboxEventPublisher {

    void publish(OutboxEvent event);
}
