package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.LapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Lap operations.
 * See: implementation_steps_plan.md § Етап 7.3-7.4.
 */
@Repository
public interface LapRepository extends JpaRepository<Lap, LapId> {

    /**
     * Find all laps for a session and car, ordered by lap number.
     */
    List<Lap> findBySessionUidAndCarIndexOrderByLapNumberAsc(Long sessionUid, Short carIndex);

    /**
     * Find all laps for a session (all cars).
     */
    List<Lap> findBySessionUidOrderByCarIndexAscLapNumberAsc(Long sessionUid);
}
