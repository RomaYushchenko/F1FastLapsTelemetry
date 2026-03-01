package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket server → client: new session event (FTLP, PENA, SCAR, etc.).
 * Sent when EventProcessor persists a new event. Client appends to timeline.
 * Block E — optional 19.10.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsSessionEventMessage {

    public static final String TYPE = "SESSION_EVENT";

    private String type;
    private SessionEventDto event;
}
