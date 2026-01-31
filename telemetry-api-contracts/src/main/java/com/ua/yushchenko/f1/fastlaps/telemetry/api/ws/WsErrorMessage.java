package com.ua.yushchenko.f1.fastlaps.telemetry.api.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket server → client: error notification.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 5.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsErrorMessage {

    public static final String TYPE = "ERROR";

    /** Message type: "ERROR" */
    private String type;
    
    /** Error code (e.g., INVALID_SUBSCRIPTION) */
    private String code;
    
    /** Human-readable error message */
    private String message;
}
