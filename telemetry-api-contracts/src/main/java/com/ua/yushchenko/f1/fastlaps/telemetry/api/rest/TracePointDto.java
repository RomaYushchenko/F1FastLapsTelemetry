package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST DTO for pedal trace (GET /api/sessions/{sessionUid}/laps/{lapNum}/trace).
 * One sample: distance (m), throttle (0–1), brake (0–1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TracePointDto {

    private double distance;
    private double throttle;
    private double brake;
}
