package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bounding box for track layout (for client viewBox). Optional in GET /api/tracks/{trackId}/layout.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayoutBoundsDto {

    private Double minX;
    private Double minY;
    private Double maxX;
    private Double maxY;
}
