package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for GET /api/tracks/{trackId}/layout. Plan: block-f-live-track-map.md.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayoutResponseDto {

    private Integer trackId;
    private List<TrackLayoutPointDto> points;
    private TrackLayoutBoundsDto bounds;
}
