package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

/**
 * Status for track layout recording / availability.
 *
 * status = "READY" | "RECORDING" | "NOT_AVAILABLE"
 */
public record TrackLayoutStatusDto(
        int trackId,
        String status,
        int pointsCollected,
        String source
) {
}

