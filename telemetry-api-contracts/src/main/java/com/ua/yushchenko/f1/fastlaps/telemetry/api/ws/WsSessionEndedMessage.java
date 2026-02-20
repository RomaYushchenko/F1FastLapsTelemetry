package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket server → client: session ended notification.
 * Uses session id (UUID string) for consistency with REST API.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 4.5.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsSessionEndedMessage {

    public static final String TYPE = "SESSION_ENDED";

    private String type;
    /** Session public id (UUID string). Same as REST GET /api/sessions/{id}. */
    private String sessionId;
    /** EVENT_SEND, NO_DATA_TIMEOUT, MANUAL */
    private String endReason;
}
