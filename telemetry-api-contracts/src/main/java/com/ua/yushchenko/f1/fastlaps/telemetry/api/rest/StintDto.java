package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for one tyre stint (GET /api/sessions/{sessionUid}/stints).
 * Consecutive laps with the same compound form one stint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StintDto {

    /** 1-based stint index. */
    private int stintIndex;
    /** F1 25 compound code (e.g. 16=C5, 18=C3). */
    private Integer compound;
    /** First lap number of the stint. */
    private int startLap;
    /** Number of laps in the stint. */
    private int lapCount;
    /** Average lap time in ms over valid laps in the stint. Optional (null) if none. */
    private Integer avgLapTimeMs;
    /** Degradation indicator: "high", "medium", "low", or null. UI shows "—" when null. */
    private String degradationIndicator;
}
