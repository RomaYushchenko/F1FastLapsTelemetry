package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.publisher;

import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.PublishException;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher.TelemetryPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Decorator that adds retry logic with exponential backoff to a {@link TelemetryPublisher}.
 * Wraps another publisher and retries failed publish operations.
 */
@Slf4j
@RequiredArgsConstructor
public class RetryingPublisher implements TelemetryPublisher {

    private final TelemetryPublisher delegate;
    private final int maxAttempts;
    private final long initialBackoffMs;

    @Override
    public void publish(String topic, String key, Object value) {
        int attempt = 1;
        long backoffMs = initialBackoffMs;

        while (attempt <= maxAttempts) {
            try {
                delegate.publish(topic, key, value);
                if (attempt > 1) {
                    log.info("Successfully published after {} attempts to topic={}", attempt, topic);
                }
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    log.error("Failed to publish to topic={} after {} attempts", topic, maxAttempts, e);
                    throw new PublishException("Failed after " + maxAttempts + " attempts", e);
                }

                log.warn("Publish attempt {} failed for topic={}, retrying in {}ms: {}",
                        attempt, topic, backoffMs, e.getMessage());

                sleep(backoffMs);
                backoffMs *= 2; // Exponential backoff
                attempt++;
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublishException("Interrupted during retry backoff", e);
        }
    }
}
