package com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception;

/**
 * Thrown when a session cannot be found by public id or session UID.
 * REST: maps to 404 + RestErrorResponse (SESSION_NOT_FOUND).
 * WebSocket: maps to WsErrorMessage (SESSION_NOT_FOUND).
 * See: implementation_phases.md Phase 1.2, rest_web_socket_api_contracts_f_1_telemetry.md § 5.
 */
public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
