package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

/**
 * Session state machine states.
 * See: state_machines_specification_f_1_telemetry.md § 2.2.1.
 */
public enum SessionState {
    /**
     * Initial state: session created but not yet active.
     * Entered: on first packet received (before SSTA event).
     */
    INIT,

    /**
     * Active state: session is running, data is flowing.
     * Entered: on SSTA (Session Started) event.
     */
    ACTIVE,

    /**
     * Ending state: session received END event, flushing data.
     * Entered: on SEND (Session Ended) event or NO_DATA_TIMEOUT.
     */
    ENDING,

    /**
     * Terminal state: session fully finalized, no more processing.
     * Entered: after ENDING → all data flushed, WebSocket closed.
     */
    TERMINAL
}
