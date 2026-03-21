package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.ProcessedPacket;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.ProcessedPacketId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Repository for idempotency tracking.
 * See: implementation_steps_plan.md § Етап 5.2.
 */
@Repository
public interface ProcessedPacketRepository extends JpaRepository<ProcessedPacket, ProcessedPacketId> {

    /**
     * Removes all idempotency rows for a session (after terminal lifecycle).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProcessedPacket p where p.sessionUid = :sessionUid")
    int deleteBySessionUid(@Param("sessionUid") long sessionUid);

    /**
     * Removes rows older than the cutoff (time-based retention).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ProcessedPacket p where p.processedAt < :cutoff")
    int deleteByProcessedAtBefore(@Param("cutoff") Instant cutoff);
}
