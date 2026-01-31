package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

/**
 * Standard error codes for REST and WebSocket API.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 5.
 */
public enum ErrorCode {
    /** Session not found (HTTP 404) */
    SESSION_NOT_FOUND,
    
    /** Invalid request parameters (HTTP 400) */
    INVALID_REQUEST,
    
    /** Invalid WebSocket subscription */
    INVALID_SUBSCRIPTION,
    
    /** Internal server error (HTTP 500) */
    INTERNAL_ERROR,
    
    /** Session is not active */
    SESSION_NOT_ACTIVE,
    
    /** Unsupported operation */
    UNSUPPORTED_OPERATION
}
