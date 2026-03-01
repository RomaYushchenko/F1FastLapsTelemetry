package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    /** User-facing display name (max 64 chars). Defaults to public_id at insert. */
    @Column(name = "session_display_name", nullable = false, length = 64)
    private String sessionDisplayName;

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

    /** Player car index (0–19) from F1 header; ingest sends only this car. Null until first telemetry. */
    @Column(name = "player_car_index")
    private Short playerCarIndex;

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

    /**
     * Read-only association for JPA criteria joins (e.g. sort by finishing position).
     * Do not use for lazy loading; only for query building in SessionListSpecification.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_uid", referencedColumnName = "session_uid", insertable = false, updatable = false)
    @Builder.Default
    private List<SessionFinishingPosition> finishingPositions = new ArrayList<>();

    /**
     * Read-only association for JPA criteria joins (e.g. sort by best lap).
     * Do not use for lazy loading; only for query building in SessionListSpecification.
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_uid", referencedColumnName = "session_uid", insertable = false, updatable = false)
    @Builder.Default
    private List<SessionSummary> summaries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (sessionDisplayName == null) {
            sessionDisplayName = publicId.toString();
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
