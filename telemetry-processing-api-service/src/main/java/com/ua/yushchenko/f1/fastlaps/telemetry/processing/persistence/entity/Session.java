package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Session entity (telemetry.sessions table).
 * See: implementation_steps_plan.md § Етап 7.1.
 */
@Entity
@Table(name = "sessions", schema = "telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Column(name = "packet_format", nullable = false)
    private Short packetFormat;

    @Column(name = "game_major_version", nullable = false)
    private Short gameMajorVersion;

    @Column(name = "game_minor_version", nullable = false)
    private Short gameMinorVersion;

    @Column(name = "session_type")
    private Short sessionType;

    @Column(name = "track_id")
    private Short trackId;

    @Column(name = "track_length_m")
    private Integer trackLengthM;

    @Column(name = "total_laps")
    private Short totalLaps;

    @Column(name = "ai_difficulty")
    private Short aiDifficulty;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "end_reason", length = 32)
    private String endReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
