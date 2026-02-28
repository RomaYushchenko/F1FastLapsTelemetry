package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One corner in a track map (for GET corner-maps/latest). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackCornerItemDto {

    private int cornerIndex;
    private String name;
    private Float startDistanceM;
    private Float endDistanceM;
    private Float apexDistanceM;
}
