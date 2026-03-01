package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for ERS-by-lap (GET /api/sessions/{sessionUid}/ers-by-lap).
 * One value per lap: ERS store energy at lap end (0–100%).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErsByLapDto {

    /** Lap number (1-based). */
    private Integer lapNumber;
    /** ERS energy store at lap end, 0–100%. */
    private Integer ersStorePercentEnd;
}
