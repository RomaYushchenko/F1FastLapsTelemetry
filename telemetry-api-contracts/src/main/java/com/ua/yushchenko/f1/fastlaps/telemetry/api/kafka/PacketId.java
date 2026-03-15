package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

/**
 * UDP packet type (Kafka contract).
 * See: kafka_contracts_f_1_telemetry.md § 4.
 */
public enum PacketId {
    CAR_TELEMETRY,
    LAP_DATA,
    CAR_STATUS,
    CAR_DAMAGE,
    SESSION,
    EVENT,
    MOTION,
    PARTICIPANTS
}
