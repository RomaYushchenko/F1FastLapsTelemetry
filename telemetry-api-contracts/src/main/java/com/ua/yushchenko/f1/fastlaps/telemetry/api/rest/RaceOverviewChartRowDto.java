package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One lap row for multi-series charts. {@code values} order matches {@link SessionRaceOverviewDto#getDrivers()}.
 * Null elements mean no data for that driver on this lap.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceOverviewChartRowDto {

    private int lapNumber;
    /** Position (1-based) or gap in seconds; null per driver when unknown. */
    private List<Double> values;
}
