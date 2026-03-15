package com.ua.yushchenko.f1.fastlaps.telemetry.processing.model;

/**
 * Reusable 3D point in world coordinates.
 *
 * x = worldPositionX (horizontal left/right)
 * y = worldPositionY (elevation)
 * z = worldPositionZ (horizontal depth)
 *
 * Used for track layout points, sector boundaries, and anywhere coordinates are stored or passed.
 */
public record Point3D(double x, double y, double z) {
}
