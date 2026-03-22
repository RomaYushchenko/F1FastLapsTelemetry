package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated post-session race overview: leaderboard, driver colors, position and gap-to-leader chart series.
 * <p>
 * Gap chart: cumulative race time behind the on-track leader at each lap (leader = minimum cumulative time
 * among drivers who completed that lap); values in seconds, leader 0.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRaceOverviewDto {

    private String sessionUid;
    private List<LeaderboardEntryDto> entries;
    private List<RaceOverviewDriverDto> drivers;
    /** Position per lap; Y axis typically inverted so P1 is top. */
    private List<RaceOverviewChartRowDto> positionChartRows;
    /** Cumulative gap to leader in seconds per lap. */
    private List<RaceOverviewChartRowDto> gapChartRows;
}
