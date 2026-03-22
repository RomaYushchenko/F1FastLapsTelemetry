package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Driver metadata for race overview charts (aligned order with chart row value lists).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceOverviewDriverDto {

    private int carIndex;
    private String displayLabel;
    /** Hex color for chart line and legend, e.g. #00E5FF */
    private String colorHex;
}
