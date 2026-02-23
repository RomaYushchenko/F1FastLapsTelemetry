package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.LapCornerMetrics;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.LapCornerMetricsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for lap_corner_metrics. Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Repository
public interface LapCornerMetricsRepository extends JpaRepository<LapCornerMetrics, LapCornerMetricsId> {

    List<LapCornerMetrics> findBySessionUidAndCarIndexAndLapNumberOrderByCornerIndexAsc(
            Long sessionUid, Short carIndex, Short lapNumber);
}
