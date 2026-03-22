package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry in the live leaderboard (GET /api/sessions/active/leaderboard, WebSocket LEADERBOARD)
 * or post-session leaderboard (GET /api/sessions/{id}/leaderboard).
 * <p>
 * {@code gap} is cumulative race time vs P1 (sum of valid completed laps): {@code LEAD} for P1,
 * {@code +M:SS.xx} when behind on total time, {@code +0.00} when tied, {@code —} if totals missing
 * or car total is less than leader total (inconsistent data).
 * {@code lastLapGap} uses the same display style for the last completed lap only vs P1's last lap.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDto {

    /** Race position (1-based). */
    private Integer position;
    /** Car index (0–19). */
    private Integer carIndex;
    /** Driver display label (e.g. "VER"); fallback "Car N" if null from backend. */
    private String driverLabel;
    /** Tyre compound for display: "S", "M", or "H". */
    private String compound;
    /** Cumulative race gap vs P1: "LEAD", "+delta", "+0.00", or "—". */
    private String gap;
    /** Last completed lap vs P1 last lap: "LEAD", "+delta", "+0.00", or "—". */
    private String lastLapGap;
    /** Best lap time in ms (from session summary when available, else min valid lap); null if none. */
    private Integer bestLapTimeMs;
    /** Sum of valid completed lap times in ms; null if no completed laps. */
    private Integer totalRaceTimeMs;
    /** Last completed lap time in ms; null if none. */
    private Integer lastLapTimeMs;
    /** Sector 1 time of last lap (ms); null if none. */
    private Integer sector1Ms;
    /** Sector 2 time of last lap (ms); null if none. */
    private Integer sector2Ms;
    /** Sector 3 time of last lap (ms); null if none. */
    private Integer sector3Ms;
}
