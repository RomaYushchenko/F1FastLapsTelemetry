package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.ProcessedPacket;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.ProcessedPacketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotency service: checks and records processed packets.
 * Uses database constraint to prevent duplicate processing (INSERT ... ON CONFLICT DO NOTHING pattern).
 * See: implementation_steps_plan.md § Етап 5.3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedPacketRepository repository;

    /**
     * Check if packet was already processed and mark as processed if not.
     * Returns true if packet is new (should be processed), false if duplicate.
     */
    @Transactional
    public boolean markAsProcessed(long sessionUid, int frameIdentifier, short packetId, short carIndex) {
        try {
            ProcessedPacket packet = ProcessedPacket.builder()
                    .sessionUid(sessionUid)
                    .frameIdentifier(frameIdentifier)
                    .packetId(packetId)
                    .carIndex(carIndex)
                    .build();

            repository.save(packet);
            return true; // Successfully saved, this is a new packet
        } catch (DataIntegrityViolationException e) {
            // Duplicate key violation - packet already processed
            log.debug("Duplicate packet detected: sessionUid={}, frame={}, packetId={}, carIndex={}",
                    sessionUid, frameIdentifier, packetId, carIndex);
            return false;
        }
    }
}
