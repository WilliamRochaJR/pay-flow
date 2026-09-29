package com.payflow.transfers;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Component
public class TransferMetrics {

    private final MeterRegistry registry;

    public TransferMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    void completedAfterCommit() {
        afterCommit(() -> registry.counter("payflow.transfers.completed").increment());
    }

    void replayedAfterCommit() {
        afterCommit(() -> registry.counter("payflow.transfers.replayed").increment());
    }

    void rejected(String reason) {
        registry.counter("payflow.transfers.rejected", "reason", reason).increment();
    }

    void failed() {
        registry.counter("payflow.transfers.failed").increment();
    }

    void recordDuration(long startedAtNanos, String outcome) {
        Timer.builder("payflow.transfers.duration")
                .description("Duration of transfer creation attempts")
                .tag("outcome", outcome)
                .register(registry)
                .record(Duration.ofNanos(System.nanoTime() - startedAtNanos));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
