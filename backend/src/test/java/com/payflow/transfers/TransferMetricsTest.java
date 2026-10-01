package com.payflow.transfers;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransferMetricsTest {

    @Test
    void recordsEveryBoundedReversalOutcome() {
        var registry = new SimpleMeterRegistry();
        var metrics = new TransferMetrics(registry);

        metrics.reversalCompletedAfterCommit();
        metrics.reversalReplayedAfterCommit();
        metrics.reversalRejected("reversal-already-exists");
        metrics.reversalFailed();
        metrics.recordReversalDuration(System.nanoTime(), "completed");

        assertThat(registry.counter("payflow.reversals.completed").count()).isEqualTo(1);
        assertThat(registry.counter("payflow.reversals.replayed").count()).isEqualTo(1);
        assertThat(registry.counter(
                "payflow.reversals.rejected", "reason", "reversal-already-exists").count()).isEqualTo(1);
        assertThat(registry.counter("payflow.reversals.failed").count()).isEqualTo(1);
        assertThat(registry.timer("payflow.reversals.duration", "outcome", "completed").count()).isEqualTo(1);
    }
}
