package com.payflow.events.audit;

import com.payflow.events.EventMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RetryListener;

class AuditRetryListener implements RetryListener {

    private final long retryAttempts;
    private final EventMetrics metrics;

    AuditRetryListener(long retryAttempts, EventMetrics metrics) {
        this.retryAttempts = retryAttempts;
        this.metrics = metrics;
    }

    @Override
    public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
        if (deliveryAttempt <= retryAttempts) {
            metrics.auditRetry();
        }
    }

    @Override
    public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
        metrics.auditDeadLettered();
    }
}
