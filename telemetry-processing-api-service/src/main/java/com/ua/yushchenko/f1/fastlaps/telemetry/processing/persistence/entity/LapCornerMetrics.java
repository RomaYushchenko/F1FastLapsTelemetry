package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-lap corner metrics (telemetry.lap_corner_metrics).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Entity
@Table(name = "lap_corner_metrics", schema = "telemetry")
@IdClass(LapCornerMetricsId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LapCornerMetrics {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Id
    @Column(name = "lap_number", nullable = false)
    private Short lapNumber;

    @Id
    @Column(name = "corner_index", nullable = false)
    private Short cornerIndex;

    @Column(name = "entry_speed_kph")
    private Short entrySpeedKph;

    @Column(name = "apex_speed_kph")
    private Short apexSpeedKph;

    @Column(name = "exit_speed_kph")
    private Short exitSpeedKph;

    @Column(name = "min_speed_kph")
    private Short minSpeedKph;

    @Column(name = "avg_speed_kph")
    private Short avgSpeedKph;

    @Column(name = "max_speed_kph")
    private Short maxSpeedKph;

    @Column(name = "duration_ms")
    private Integer durationMs;
}
