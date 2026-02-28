package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for GET /api/tracks/{trackId}/corner-maps/latest.
 * Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackCornerMapResponseDto {

    private Integer trackId;
    private Integer trackLengthM;
    private Integer version;
    private List<TrackCornerItemDto> corners;
}
