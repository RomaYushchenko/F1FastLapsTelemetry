package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * REST DTO for session (GET /api/sessions, GET /api/sessions/{id}).
 * Uses UUID as public id to avoid JS number precision issues with F1 session UID.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 3.1, § 8.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDto {

    /** Public session identifier (UUID). Use in URLs and WebSocket subscribe. */
    private String id;
    /** User-facing display name (max 64 chars). Editable via PATCH /api/sessions/{id}. */
    private String sessionDisplayName;
    private String sessionType;
    private Integer trackId;
    /** Human-readable track name (from F1Track). Prefer for display; use trackId for filtering. */
    private String trackDisplayName;
    private Integer trackLengthM;
    private Integer totalLaps;
    private Integer aiDifficulty;
    private Instant startedAt;
    private Instant endedAt;
    /** EVENT_SEND, NO_DATA_TIMEOUT, MANUAL */
    private String endReason;
    /** ACTIVE or FINISHED (required in all responses per § 8.2) */
    private SessionState state;
    /** Player car index (0–19) from F1; use this for laps/summary/tyre-wear so data is for the driver. */
    private Integer playerCarIndex;
    /** Finishing position (race position at session end). Null if session active or no LapData received. */
    private Integer finishingPosition;
    /** Participants (car indices with data) for Driver Comparison. Included in GET /api/sessions/{id}; may be omitted in list. */
    private List<SessionParticipantDto> participants;

    /**
     * Best lap time in milliseconds for the player car.
     * Filled from SessionSummary for carIndex = playerCarIndex when available; null otherwise.
     */
    private Integer bestLapTimeMs;

    /**
     * Total session time in milliseconds.
     * For FINISHED sessions computed as endedAt - startedAt; null when timestamps are missing or session is ACTIVE.
     */
    private Long totalTimeMs;
}
