package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

/**
 * Session state for REST API responses.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 8.1.
 */
public enum SessionState {
    /**
     * Session is active, data is flowing.
     */
    ACTIVE,

    /**
     * Session is finished (EVENT_SEND or NO_DATA_TIMEOUT).
     */
    FINISHED
}
