package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Raw car telemetry sample (telemetry.car_telemetry_raw).
 * Used for pedal trace: throttle/brake per lap distance.
 * See: PEDAL_TRACE_FEATURE_ANALYSIS_AND_PLAN.md, infra/init-db/05-car-telemetry-raw.sql.
 */
@Entity
@Table(name = "car_telemetry_raw", schema = "telemetry")
@IdClass(CarTelemetryRawId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarTelemetryRaw {

    @Id
    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "frame_identifier", nullable = false)
    private Integer frameIdentifier;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Column(name = "speed_kph")
    private Short speedKph;

    @Column(name = "throttle")
    private Float throttle;

    @Column(name = "steer")
    private Float steer;

    @Column(name = "brake")
    private Float brake;

    @Column(name = "gear")
    private Short gear;

    @Column(name = "engine_rpm")
    private Integer engineRpm;

    @Column(name = "drs")
    private Short drs;

    @Column(name = "session_time_s")
    private Float sessionTimeS;

    @Column(name = "lap_number")
    private Short lapNumber;

    @Column(name = "lap_distance_m")
    private Float lapDistanceM;
}
