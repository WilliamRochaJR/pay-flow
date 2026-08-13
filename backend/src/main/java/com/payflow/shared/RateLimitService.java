package com.payflow.shared;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final AtomicInteger operations = new AtomicInteger();

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public Decision consume(String key, int limit, Duration duration) {
        Instant now = clock.instant();
        if (operations.incrementAndGet() % 1_000 == 0) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetAt()));
        }
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.resetAt())) {
                return new Window(1, now.plus(duration));
            }
            return new Window(current.count() + 1, current.resetAt());
        });
        long remainingMillis = Duration.between(now, window.resetAt()).toMillis();
        long retryAfter = Math.max(1, (remainingMillis + 999) / 1_000);
        return new Decision(window.count() <= limit, retryAfter);
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private record Window(int count, Instant resetAt) {
    }
}
