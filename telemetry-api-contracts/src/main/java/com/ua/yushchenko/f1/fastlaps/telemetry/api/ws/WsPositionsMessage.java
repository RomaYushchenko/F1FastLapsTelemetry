package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.CarPositionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebSocket server → client: live positions of all cars (B9).
 * Same topic as SNAPSHOT/LEADERBOARD: /topic/live/{sessionId}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsPositionsMessage {

    public static final String TYPE = "POSITIONS";

    private String type;
    private List<CarPositionDto> positions;
}
