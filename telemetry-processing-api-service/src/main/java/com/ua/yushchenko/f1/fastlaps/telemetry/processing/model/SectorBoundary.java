package com.ua.yushchenko.f1.fastlaps.telemetry.processing.model;

/**
 * Sector boundary: sector index (1, 2, 3), world coordinates, and optional point index.
 * When pointIndex is set (sector 2 and 3), frontend splits track by index so 1→2→3→1 connects correctly.
 * Stored in track_layout.sector_boundaries (jsonb).
 */
public record SectorBoundary(int sector, double x, double y, double z, Integer pointIndex) {

    /** Constructor without pointIndex for backward compatibility (JSON/DB). */
    public SectorBoundary(int sector, double x, double y, double z) {
        this(sector, x, y, z, null);
    }
}
