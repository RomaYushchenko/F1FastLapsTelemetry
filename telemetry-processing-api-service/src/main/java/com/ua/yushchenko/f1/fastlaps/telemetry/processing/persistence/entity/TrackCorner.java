package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One corner in a track map (telemetry.track_corners).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Entity
@Table(name = "track_corners", schema = "telemetry")
@IdClass(TrackCornerId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackCorner {

    @Id
    @Column(name = "map_id", nullable = false)
    private Long mapId;

    @Id
    @Column(name = "corner_index", nullable = false)
    private Short cornerIndex;

    @Column(name = "start_distance_m", nullable = false)
    private Float startDistanceM;

    @Column(name = "end_distance_m", nullable = false)
    private Float endDistanceM;

    @Column(name = "apex_distance_m", nullable = false)
    private Float apexDistanceM;

    @Column(name = "direction")
    private Short direction;

    @Column(name = "name", length = 16)
    private String name;
}
