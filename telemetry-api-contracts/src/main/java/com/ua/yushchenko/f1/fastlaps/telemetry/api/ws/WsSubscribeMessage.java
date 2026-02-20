package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket client → server: subscribe to live data.
 * Uses session id (UUID string) as the public identifier.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 4.4.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsSubscribeMessage {

    public static final String TYPE = "SUBSCRIBE";

    private String type;
    /** Session public id (UUID string). Same as REST GET /api/sessions/{id}. */
    private String sessionId;
    private int carIndex;
}
