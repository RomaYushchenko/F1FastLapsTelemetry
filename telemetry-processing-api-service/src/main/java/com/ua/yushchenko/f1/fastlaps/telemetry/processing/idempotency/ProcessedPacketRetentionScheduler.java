package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.ProcessedPacketRetentionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Periodically purges old rows from {@code telemetry.processed_packets}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedPacketRetentionScheduler {

    private final ProcessedPacketRetentionService processedPacketRetentionService;
    private final ProcessedPacketRetentionProperties properties;

    @Scheduled(
            fixedDelayString = "${telemetry.idempotency.processed-packets.purge-interval-ms:120000}",
            initialDelayString = "${telemetry.idempotency.processed-packets.purge-interval-ms:120000}"
    )
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(properties.getRetention());
        int deleted = processedPacketRetentionService.deleteExpiredBefore(cutoff);
        log.debug("purgeExpired: completed deletedRows={}, cutoff={}", deleted, cutoff);
    }
}
