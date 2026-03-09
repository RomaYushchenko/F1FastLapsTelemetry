package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Export format for a single track layout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackLayoutExportDto {

    private int exportVersion;
    private String exportedAt;
    private int trackId;
    private String trackName;
    private int version;
    private String source;
    private List<TrackLayoutPointDto> points;
    private TrackLayoutBoundsExportDto bounds;
    private List<SectorBoundaryDto> sectorBoundaries;
}

