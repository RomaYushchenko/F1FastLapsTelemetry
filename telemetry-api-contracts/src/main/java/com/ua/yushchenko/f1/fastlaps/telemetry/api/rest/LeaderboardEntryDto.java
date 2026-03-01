package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry in the live leaderboard (GET /api/sessions/active/leaderboard, WebSocket LEADERBOARD).
 * Block E — Live leaderboard.
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
    /** Gap to leader: "LEAD" for P1, "+1.234" for others, or "—" if no lap time yet. */
    private String gap;
    /** Last completed lap time in ms; null if none. */
    private Integer lastLapTimeMs;
    /** Sector 1 time of last lap (ms); null if none. */
    private Integer sector1Ms;
    /** Sector 2 time of last lap (ms); null if none. */
    private Integer sector2Ms;
    /** Sector 3 time of last lap (ms); null if none. */
    private Integer sector3Ms;
}
