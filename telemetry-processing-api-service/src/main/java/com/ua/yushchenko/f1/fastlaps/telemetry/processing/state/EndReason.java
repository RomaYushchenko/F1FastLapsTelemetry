package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

/**
 * Reason for session termination.
 * See: telemetry_error_and_lifecycle_contract.md § 3.2.
 */
public enum EndReason {
    /**
     * Normal session end: received SEND event from game.
     */
    EVENT_SEND,

    /**
     * Session timeout: no data received for configured timeout period.
     */
    NO_DATA_TIMEOUT,

    /**
     * Manual session termination (e.g., admin action, system shutdown).
     */
    MANUAL
}
