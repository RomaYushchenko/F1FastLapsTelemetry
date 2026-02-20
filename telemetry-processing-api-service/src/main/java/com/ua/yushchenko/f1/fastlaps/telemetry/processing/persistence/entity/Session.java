package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Session entity (telemetry.sessions table).
 * <p>
 * <b>Link session_uid ↔ public_id:</b> One row has both identifiers; they are 1:1.
 * - {@code session_uid}: PK from F1 game telemetry.
 * - {@code public_id}: UUID generated at insert ({@link #onCreate()}) for REST/UI/WebSocket.
 * REST and WebSocket use the same id: when present use {@code public_id}, else {@code session_uid}.
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

    /** Public identifier for API/UI (UUID). Tied 1:1 to session_uid in this row. */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

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
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
