package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for GET /api/tracks/{trackId}/layout.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayoutResponseDto {

    private Integer trackId;
    private List<TrackLayoutPointDto> points;
    private TrackLayoutBoundsDto bounds;
    /**
     * Layout source: STATIC = manually created; RECORDED = auto-recorded from UDP telemetry.
     */
    private String source;
    /**
     * Optional sector boundaries in world coordinates (start of each sector). Null for static tracks.
     */
    private List<SectorBoundaryDto> sectorBoundaries;
}
