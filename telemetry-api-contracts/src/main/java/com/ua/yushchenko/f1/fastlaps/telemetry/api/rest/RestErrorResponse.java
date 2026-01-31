package com.ua.yushchenko.f1.fastlaps.telemetry.api.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REST API error response.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 5.1.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestErrorResponse {

    /** Error code (e.g., SESSION_NOT_FOUND) */
    private String error;
    
    /** Human-readable error message */
    private String message;
}
