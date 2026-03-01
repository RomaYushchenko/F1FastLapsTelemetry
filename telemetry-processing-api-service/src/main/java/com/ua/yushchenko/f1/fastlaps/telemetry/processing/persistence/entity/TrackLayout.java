package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2D track layout (centreline/outline points) for Live Track Map. Block F — B8.
 */
@Entity
@Table(name = "track_layout", schema = "telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayout {

    @Id
    @Column(name = "track_id", nullable = false)
    private Short trackId;

    @Column(name = "points", nullable = false, columnDefinition = "jsonb")
    private String pointsJson;

    @Column(name = "version", nullable = false)
    private Short version;

    @Column(name = "min_x")
    private Double minX;

    @Column(name = "min_y")
    private Double minY;

    @Column(name = "max_x")
    private Double maxX;

    @Column(name = "max_y")
    private Double maxY;
}
