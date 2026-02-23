package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackCornerMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for track_corner_maps. Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Repository
public interface TrackCornerMapRepository extends JpaRepository<TrackCornerMap, Long> {

    List<TrackCornerMap> findByTrackIdAndTrackLengthMOrderByVersionDesc(
            Short trackId, Integer trackLengthM);

    /** Latest map for track (highest version). */
    default Optional<TrackCornerMap> findLatestByTrackIdAndTrackLengthM(Short trackId, Integer trackLengthM) {
        return findByTrackIdAndTrackLengthMOrderByVersionDesc(trackId, trackLengthM)
                .stream()
                .findFirst();
    }
}
