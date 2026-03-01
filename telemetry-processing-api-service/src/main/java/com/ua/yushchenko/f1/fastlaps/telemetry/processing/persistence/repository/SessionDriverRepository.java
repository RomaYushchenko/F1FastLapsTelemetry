package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriverId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for session driver display labels (leaderboard, events).
 * Block E — Live leaderboard.
 */
@Repository
public interface SessionDriverRepository extends JpaRepository<SessionDriver, SessionDriverId> {

    List<SessionDriver> findBySessionUidOrderByCarIndexAsc(Long sessionUid);

    Optional<SessionDriver> findBySessionUidAndCarIndex(Long sessionUid, Short carIndex);
}
