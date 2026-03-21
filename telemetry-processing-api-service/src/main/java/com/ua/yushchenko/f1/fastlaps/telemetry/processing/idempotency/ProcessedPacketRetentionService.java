package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.ProcessedPacketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Deletes idempotency rows by age. Used by the scheduled purge ({@link ProcessedPacketRetentionScheduler}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedPacketRetentionService {

    private final ProcessedPacketRepository processedPacketRepository;

    /**
     * Deletes all processed_packet rows with {@code processed_at} strictly before the cutoff.
     *
     * @return number of rows removed
     */
    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        int deleted = processedPacketRepository.deleteByProcessedAtBefore(cutoff);
        log.debug("deleteExpiredBefore: deletedRows={}, cutoff={}", deleted, cutoff);
        return deleted;
    }
}
