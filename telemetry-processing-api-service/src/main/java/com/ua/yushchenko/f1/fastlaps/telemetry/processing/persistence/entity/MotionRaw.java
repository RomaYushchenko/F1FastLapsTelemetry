package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Raw motion sample (telemetry.motion_raw). Joined with car_telemetry_raw by (session_uid, frame_identifier, car_index).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 4.
 */
@Entity
@Table(name = "motion_raw", schema = "telemetry")
@IdClass(MotionRawId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotionRaw {

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

    @Column(name = "g_force_lateral")
    private Float gForceLateral;

    @Column(name = "yaw")
    private Float yaw;

    @Column(name = "world_pos_x")
    private Float worldPosX;

    @Column(name = "world_pos_z")
    private Float worldPosZ;
}
