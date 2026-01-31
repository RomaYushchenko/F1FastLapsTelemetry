package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket client → server: unsubscribe from live telemetry.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 4.4.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsUnsubscribeMessage {

    public static final String TYPE = "UNSUBSCRIBE";

    /** Message type: "UNSUBSCRIBE" */
    private String type;
}
