package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One 3D point in a track layout (for GET /api/tracks/{trackId}/layout).
 *
 * x = worldPositionX (horizontal)
 * y = worldPositionY (elevation)
 * z = worldPositionZ (horizontal depth)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayoutPointDto {

    private Double x;
    private Double y;
    private Double z;
}
