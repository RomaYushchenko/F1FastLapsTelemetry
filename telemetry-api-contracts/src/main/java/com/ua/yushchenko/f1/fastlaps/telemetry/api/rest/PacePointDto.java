package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for pace chart (GET /api/sessions/{sessionUid}/pace).
 * One point per lap: lap number and lap time in ms.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacePointDto {

    private int lapNumber;
    private int lapTimeMs;
}
