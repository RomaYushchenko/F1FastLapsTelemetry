package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPositionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for session finishing positions (race position at session end).
 * Plan: 03-session-page.md Etap 3; 04-session-summary-page.md Etap 1 (leader).
 */
public interface SessionFinishingPositionRepository extends JpaRepository<SessionFinishingPosition, SessionFinishingPositionId> {

    Optional<SessionFinishingPosition> findBySessionUidAndCarIndex(Long sessionUid, Short carIndex);

    /** All finishing positions for a session, ordered by position ascending (leader first). */
    List<SessionFinishingPosition> findBySessionUidOrderByFinishingPositionAsc(Long sessionUid);
}
