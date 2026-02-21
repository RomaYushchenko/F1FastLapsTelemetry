package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

/**
 * Decorator that adds rate limiting to a {@link TelemetryPublisher}.
 * Uses Guava's RateLimiter to throttle publish operations.
 */
@Slf4j
public class ThrottlingPublisher implements TelemetryPublisher {

    private final TelemetryPublisher delegate;
    private final RateLimiter rateLimiter;

    /**
     * Creates a throttling publisher.
     *
     * @param delegate the underlying publisher
     * @param permitsPerSecond maximum number of publish operations per second
     */
    public ThrottlingPublisher(TelemetryPublisher delegate, double permitsPerSecond) {
        this.delegate = delegate;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    @Override
    public void publish(String topic, String key, Object value) {
        double waitTime = rateLimiter.acquire();
        if (waitTime > 0) {
            log.debug("Throttled publish to topic={}, waited {}s", topic, waitTime);
        }
        delegate.publish(topic, key, value);
    }
}
