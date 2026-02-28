package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for speed-vs-distance trace (GET /api/sessions/{sessionUid}/laps/{lapNum}/speed-trace).
 * One sample: lap distance (m) and speed (kph). Ordered by distanceM.
 * Plan: 13-session-summary-speed-corner-graph.md Phase 1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeedTracePointDto {

    /** Lap distance in metres. */
    private Float distanceM;
    /** Speed in km/h. */
    private Integer speedKph;
}
