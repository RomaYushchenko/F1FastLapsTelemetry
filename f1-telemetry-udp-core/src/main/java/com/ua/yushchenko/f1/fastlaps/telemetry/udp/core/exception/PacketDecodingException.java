package com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.exception;

/**
 * Exception thrown when packet decoding fails.
 */
public class PacketDecodingException extends RuntimeException {
    
    public PacketDecodingException(String message) {
        super(message);
    }
    
    public PacketDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
