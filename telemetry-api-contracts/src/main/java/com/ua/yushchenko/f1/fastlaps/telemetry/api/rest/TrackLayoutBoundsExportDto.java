package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bounds for exported track layout including elevation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackLayoutBoundsExportDto {

    private double minX;
    private double maxX;
    private double minZ;
    private double maxZ;
    private double minElev;
    private double maxElev;
}

