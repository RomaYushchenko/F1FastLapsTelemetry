package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

/**
 * Session event code (Kafka contract).
 * See: kafka_contracts, state_machines_specification.
 */
public enum EventCode {
    /** Session started */
    SSTA,
    /** Session ended */
    SEND,
    /** Session metadata (type, track) for existing session (e.g. after implicit start) */
    SESSION_INFO,
    /** Session timeout (no data) */
    SESSION_TIMEOUT,
    /** Flashback / rewind */
    FLBK
}
