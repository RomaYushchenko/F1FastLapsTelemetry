package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for tyre wear chart (GET /api/sessions/{sessionUid}/tyre-wear).
 * One point per lap: lap number and wear percentage per wheel (FL, FR, RL, RR).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TyreWearPointDto {

    private int lapNumber;
    private Float wearFL;
    private Float wearFR;
    private Float wearRL;
    private Float wearRR;
}
