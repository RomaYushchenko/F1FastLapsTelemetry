package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TrackLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for track_layout (2D map points). Block F — B8.
 */
@Repository
public interface TrackLayoutRepository extends JpaRepository<TrackLayout, Short> {
}
