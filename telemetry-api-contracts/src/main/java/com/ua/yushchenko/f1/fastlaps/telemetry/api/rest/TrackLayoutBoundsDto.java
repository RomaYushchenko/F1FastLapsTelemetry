package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bounding box for track layout (for client viewBox).
 *
 * minX/maxX = worldPositionX bounds
 * minZ/maxZ = worldPositionZ bounds (horizontal plane)
 * minElev/maxElev = worldPositionY bounds (elevation)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLayoutBoundsDto {

    private Double minX;
    private Double maxX;
    private Double minZ;
    private Double maxZ;
    private Double minElev;
    private Double maxElev;
}
