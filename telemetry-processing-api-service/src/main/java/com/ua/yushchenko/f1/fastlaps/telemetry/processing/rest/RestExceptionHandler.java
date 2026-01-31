package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.RestErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST controllers.
 * See: rest_web_socket_api_contracts_f_1_telemetry.md § 5.1.
 */
@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid request: {}", e.getMessage());
        
        RestErrorResponse error = RestErrorResponse.builder()
                .error(ErrorCode.INVALID_REQUEST.name())
                .message(e.getMessage())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorResponse> handleGenericException(Exception e) {
        log.error("Internal error", e);
        
        RestErrorResponse error = RestErrorResponse.builder()
                .error(ErrorCode.INTERNAL_ERROR.name())
                .message("Internal server error")
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
