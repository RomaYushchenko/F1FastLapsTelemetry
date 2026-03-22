package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Finishing position per session and car (from LapData carPosition at session end).
 * Plan: 03-session-page.md Etap 3.
 */
@Entity
@Table(name = "session_finishing_positions", schema = "telemetry")
@IdClass(SessionFinishingPositionId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionFinishingPosition {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Column(name = "finishing_position", nullable = false)
    private Integer finishingPosition;

    /** Tyre compound at session end: S, M, or H; null if unknown. */
    @Column(name = "tyre_compound", length = 1)
    private String tyreCompound;
}
