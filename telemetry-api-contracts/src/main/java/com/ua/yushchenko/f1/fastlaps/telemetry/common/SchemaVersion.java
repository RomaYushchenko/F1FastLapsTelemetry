package com.ua.yushchenko.f1.fastlaps.telemetry.common;

/**
 * Envelope contract version (Kafka).
 * See: kafka_contracts_f_1_telemetry.md § 4.
 */
public final class SchemaVersion {

    /** Current envelope version */
    public static final int CURRENT = 1;

    private SchemaVersion() {
    }
}
