package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for ERS chart (GET /api/sessions/{sessionUid}/laps/{lapNum}/ers).
 * One point per sample: lap distance and ERS energy store percentage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErsPointDto {

    /** Lap distance in metres. */
    private Float lapDistanceM;
    /** ERS energy store 0–100%. */
    private Integer energyPercent;
}
