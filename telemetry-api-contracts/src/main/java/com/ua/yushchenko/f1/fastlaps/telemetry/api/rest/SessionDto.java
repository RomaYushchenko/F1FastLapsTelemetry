package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * REST DTO for session (GET /api/sessions, GET /api/sessions/{id}).
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 3.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDto {

    private long sessionUID;
    private String sessionType;
    private Integer trackId;
    private Integer trackLengthM;
    private Integer totalLaps;
    private Integer aiDifficulty;
    private Instant startedAt;
    private Instant endedAt;
    /** EVENT_SEND, NO_DATA_TIMEOUT, MANUAL */
    private String endReason;
}
