package com.payflow.events.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.events.audit")
record EventAuditProperties(
        String consumerName,
        String consumerGroup,
        String deadLetterTopic,
        Duration retryInterval,
        long retryAttempts,
        Duration retention
) {
}
