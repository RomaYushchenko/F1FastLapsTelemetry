package com.ua.yushchenko.f1.fastlaps.telemetry.processing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Time-based retention for {@code telemetry.processed_packets}.
 * <p>
 * Short {@link #retention} weakens deduplication if Kafka redelivers messages older than this window.
 * In production, set retention at least to broker retention plus expected consumer lag.
 */
@Data
@Component
@ConfigurationProperties(prefix = "telemetry.idempotency.processed-packets")
public class ProcessedPacketRetentionProperties {

    /**
     * Rows with {@code processed_at} older than {@code now() - retention} are deleted by the scheduled purge.
     * Default: 24 hours — wide enough for typical consumer lag and broker redelivery/replay; tune down only in
     * constrained dev environments. For production, still align with topic retention plus expected worst-case lag.
     */
    private Duration retention = Duration.ofDays(1);

    /**
     * Delay between purge runs ({@code @Scheduled fixedDelayString}), in milliseconds.
     * Default: 120000 (2 minutes).
     */
    private long purgeIntervalMs = 120_000L;
}
