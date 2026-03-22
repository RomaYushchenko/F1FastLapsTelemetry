package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for a session participant (car that has laps/summary in the session).
 * Used in SessionDto.participants for Driver Comparison (Driver A / Driver B dropdowns).
 * Forward-compatible: driverId may be added when a drivers table exists.
 * See: block-g-driver-comparison.md § 2, § 4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionParticipantDto {

    /** Car index (0–19). */
    private int carIndex;
    /** Display label: driver name from {@code session_drivers} when persisted, else finishing position (e.g. "P1"), else "Car 0". */
    private String displayLabel;
}
