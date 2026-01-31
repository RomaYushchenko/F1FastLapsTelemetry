package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Lap entity (telemetry.laps table).
 * See: implementation_steps_plan.md § Етап 7.3.
 */
@Entity
@Table(name = "laps", schema = "telemetry")
@IdClass(LapId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lap {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Id
    @Column(name = "lap_number", nullable = false)
    private Short lapNumber;

    @Column(name = "lap_time_ms")
    private Integer lapTimeMs;

    @Column(name = "sector1_time_ms")
    private Integer sector1TimeMs;

    @Column(name = "sector2_time_ms")
    private Integer sector2TimeMs;

    @Column(name = "sector3_time_ms")
    private Integer sector3TimeMs;

    @Column(name = "is_invalid", nullable = false)
    private Boolean isInvalid;

    @Column(name = "penalties_seconds")
    private Short penaltiesSeconds;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isInvalid == null) {
            isInvalid = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
