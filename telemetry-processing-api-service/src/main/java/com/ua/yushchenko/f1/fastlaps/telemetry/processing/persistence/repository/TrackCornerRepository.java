package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackCorner;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackCornerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for track_corners. Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Repository
public interface TrackCornerRepository extends JpaRepository<TrackCorner, TrackCornerId> {

    List<TrackCorner> findByMapIdOrderByCornerIndexAsc(Long mapId);
}
