package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One 2D point in a track layout (for GET /api/tracks/{trackId}/layout).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayoutPointDto {

    private Double x;
    private Double y;
}
