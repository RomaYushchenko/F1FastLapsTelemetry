package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.ProcessedPacketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Deletes idempotency rows by age or by session. Used by the scheduled purge and session lifecycle.
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

    /**
     * Deletes all idempotency rows for one session (after the session reaches TERMINAL).
     *
     * @return number of rows removed
     */
    @Transactional
    public int deleteAllForSession(long sessionUid) {
        int deleted = processedPacketRepository.deleteBySessionUid(sessionUid);
        log.debug("deleteAllForSession: deletedRows={}, sessionUid={}", deleted, sessionUid);
        return deleted;
    }
}
