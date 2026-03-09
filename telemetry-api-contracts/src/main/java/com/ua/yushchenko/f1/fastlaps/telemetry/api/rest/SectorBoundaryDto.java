package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

/**
 * Sector boundary in world coordinates for track layout response.
 */
public record SectorBoundaryDto(
        int sector,
        double x,
        double y,
        double z
) {
}

