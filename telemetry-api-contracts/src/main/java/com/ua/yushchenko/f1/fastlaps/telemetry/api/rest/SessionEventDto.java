package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * REST DTO for one session event (GET /api/sessions/{sessionUid}/events).
 * Block E — Session events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEventDto {

    private Integer lap;
    private String eventCode;
    private Integer carIndex;
    /** Event-specific detail (e.g. lapTime for FTLP, penaltyType for PENA). */
    private Object detail;
    private Instant createdAt;
}
