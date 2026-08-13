package com.payflow.shared;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void rejectsRequestsOverTheLimitAndResetsAfterTheWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-13T12:00:00Z"));
        RateLimitService service = new RateLimitService(clock);

        assertThat(service.consume("login:127.0.0.1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(service.consume("login:127.0.0.1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        RateLimitService.Decision blocked = service.consume("login:127.0.0.1", 2, Duration.ofMinutes(1));
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(60);

        clock.advance(Duration.ofMinutes(1));

        assertThat(service.consume("login:127.0.0.1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
