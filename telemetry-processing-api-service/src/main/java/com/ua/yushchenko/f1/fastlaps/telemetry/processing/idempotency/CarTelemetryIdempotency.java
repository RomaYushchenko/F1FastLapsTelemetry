package com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency;

/**
 * Idempotency keys for car telemetry Kafka messages.
 */
public final class CarTelemetryIdempotency {

    /**
     * Reserved {@code car_index} in {@code processed_packets} for a whole-frame
     * {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent}
     * (single row per frame instead of one row per car slot).
     */
    public static final short BATCH_FRAME_CAR_INDEX = -1;

    private CarTelemetryIdempotency() {
    }
}
