package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SessionSummary operations.
 * See: implementation_steps_plan.md § Етап 7.5-7.6.
 */
@Repository
public interface SessionSummaryRepository extends JpaRepository<SessionSummary, SessionSummaryId> {

    /**
     * Find summary for a specific session and car.
     */
    Optional<SessionSummary> findBySessionUidAndCarIndex(Long sessionUid, Short carIndex);

    /**
     * Find all summaries for a session (all cars). Used for participants list (cars with data).
     * Block G — Driver Comparison.
     */
    List<SessionSummary> findBySessionUid(Long sessionUid);
}
