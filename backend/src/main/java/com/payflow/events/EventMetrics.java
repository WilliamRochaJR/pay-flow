package com.payflow.events;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EventMetrics {

    private final MeterRegistry registry;

    public EventMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void relayPublished() {
        registry.counter("payflow.events.relay", "outcome", "published").increment();
    }

    public void relayFailed(boolean exhausted) {
        registry.counter("payflow.events.relay", "outcome", exhausted ? "exhausted" : "retry").increment();
    }

    public void auditProcessed(boolean duplicate) {
        registry.counter("payflow.events.audit", "outcome", duplicate ? "duplicate" : "processed").increment();
    }

    public void auditRetry() {
        registry.counter("payflow.events.audit", "outcome", "retry").increment();
    }

    public void auditDeadLettered() {
        registry.counter("payflow.events.audit", "outcome", "dead-lettered").increment();
    }

    public void outboxCleaned(int count) {
        registry.counter("payflow.events.outbox.cleaned").increment(count);
    }
}
