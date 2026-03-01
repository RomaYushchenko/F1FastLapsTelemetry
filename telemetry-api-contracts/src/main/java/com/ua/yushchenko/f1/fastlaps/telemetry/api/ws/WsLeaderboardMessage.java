package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebSocket server → client: live leaderboard (all cars).
 * Sent when LapData/position/snapshot changes for the active session.
 * Block E — Live leaderboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsLeaderboardMessage {

    public static final String TYPE = "LEADERBOARD";

    private String type;
    private List<LeaderboardEntryDto> entries;
}
