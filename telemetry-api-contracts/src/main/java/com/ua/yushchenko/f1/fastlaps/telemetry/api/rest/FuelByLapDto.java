package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for fuel-by-lap (GET /api/sessions/{sessionUid}/fuel-by-lap).
 * One value per lap: fuel remaining at lap end (raw from game, in kg).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelByLapDto {

    /** Lap number (1-based). */
    private Integer lapNumber;
    /** Fuel in tank at lap end, in kg (raw from CarStatus). */
    private Float fuelKg;
}
