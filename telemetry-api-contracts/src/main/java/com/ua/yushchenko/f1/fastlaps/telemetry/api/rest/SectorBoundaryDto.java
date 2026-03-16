package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

/**
 * Sector boundary in world coordinates for track layout response.
 * pointIndex: index into the layout points array where this sector starts (optional; when set, UI splits by index).
 */
public record SectorBoundaryDto(
        int sector,
        double x,
        double y,
        double z,
        Integer pointIndex
) {

    /** Constructor without pointIndex for backward compatibility. */
    public SectorBoundaryDto(int sector, double x, double y, double z) {
        this(sector, x, y, z, null);
    }
}

