package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.ProcessedPacket;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.ProcessedPacketId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for idempotency tracking.
 * See: implementation_steps_plan.md § Етап 5.2.
 */
@Repository
public interface ProcessedPacketRepository extends JpaRepository<ProcessedPacket, ProcessedPacketId> {
}
