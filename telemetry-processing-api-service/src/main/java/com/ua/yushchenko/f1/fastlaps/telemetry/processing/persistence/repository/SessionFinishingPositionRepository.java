package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPositionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for session finishing positions (race position at session end).
 * Plan: 03-session-page.md Etap 3.
 */
public interface SessionFinishingPositionRepository extends JpaRepository<SessionFinishingPosition, SessionFinishingPositionId> {

    Optional<SessionFinishingPosition> findBySessionUidAndCarIndex(Long sessionUid, Short carIndex);
}
