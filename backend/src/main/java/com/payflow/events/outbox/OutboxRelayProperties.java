package com.payflow.events.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.events.relay")
record OutboxRelayProperties(String topic, int batchSize, Duration sendTimeout) {
}
