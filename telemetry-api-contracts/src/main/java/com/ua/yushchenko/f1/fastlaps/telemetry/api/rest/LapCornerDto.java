package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for per-corner metrics (GET /api/sessions/{sessionUid}/laps/{lapNum}/corners).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LapCornerDto {

    /** 1-based corner index along the lap. */
    private int cornerIndex;
    private Float startDistanceM;
    private Float endDistanceM;
    private Float apexDistanceM;
    private Integer entrySpeedKph;
    private Integer apexSpeedKph;
    private Integer exitSpeedKph;
    /** Optional duration in milliseconds within the corner segment. */
    private Integer durationMs;
    /** Optional display name (e.g. "T1", "T2") from track corner map when available. */
    private String name;
}
