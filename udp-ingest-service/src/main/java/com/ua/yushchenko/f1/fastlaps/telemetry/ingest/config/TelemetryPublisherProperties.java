package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for telemetry publisher.
 */
@Data
@ConfigurationProperties(prefix = "f1.telemetry.kafka")
public class TelemetryPublisherProperties {

    /**
     * Whether Kafka publishing is enabled.
     */
    private boolean enabled = false;

    /**
     * Retry configuration.
     */
    private RetryProperties retry = new RetryProperties();

    /**
     * Throttling configuration.
     */
    private ThrottleProperties throttle = new ThrottleProperties();

    @Data
    public static class RetryProperties {
        /**
         * Whether retry is enabled.
         */
        private boolean enabled = true;

        /**
         * Maximum number of retry attempts.
         */
        private int maxAttempts = 3;

        /**
         * Initial backoff delay in milliseconds.
         */
        private long initialBackoffMs = 100;
    }

    @Data
    public static class ThrottleProperties {
        /**
         * Whether throttling is enabled.
         */
        private boolean enabled = false;

        /**
         * Maximum number of publish operations per second.
         */
        private double permitsPerSecond = 100.0;
    }
}
