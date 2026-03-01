package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for session events (FTLP, PENA, SCAR, etc.).
 * Block E — Session events.
 */
@Repository
public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {

    List<SessionEvent> findBySessionUidOrderByLapAscFrameIdAsc(Long sessionUid);

    List<SessionEvent> findBySessionUidAndLapBetweenOrderByLapAscFrameIdAsc(Long sessionUid, Short fromLap, Short toLap);
}
