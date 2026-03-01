package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for one pit stop (GET /api/sessions/{sessionUid}/pit-stops).
 * Detected from compound change between consecutive laps; in/out lap times from Lap table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitStopDto {

    /** Lap number on which the car exited the pit (out-lap). */
    private int lapNumber;
    /** In-lap time in ms (lap before pit). Optional if not recorded. */
    private Integer inLapTimeMs;
    /** Pit duration in ms. Optional (null) in MVP; can be filled when LapData pit stop timer is persisted. */
    private Integer pitDurationMs;
    /** Out-lap time in ms. Optional if not recorded. */
    private Integer outLapTimeMs;
    /** F1 25 compound code before pit (e.g. 16=C5, 18=C3). */
    private Integer compoundIn;
    /** F1 25 compound code after pit. */
    private Integer compoundOut;
}
