package com.ua.yushchenko.f1.fastlaps.telemetry.udp.spring.publisher;

/**
 * Exception thrown when telemetry publishing fails after all retry attempts.
 */
public class PublishException extends RuntimeException {
    
    public PublishException(String message) {
        super(message);
    }
    
    public PublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
