package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * UDP ingest mode for packet 6 (Car Telemetry).
 */
@Data
@ConfigurationProperties(prefix = "f1.telemetry.udp.car-telemetry")
public class CarTelemetryUdpProperties {

    /**
     * PLAYER_ONLY: one {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent} (legacy).
     * BATCH_ALL_CARS: one {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent} with all slots.
     */
    private Mode mode = Mode.BATCH_ALL_CARS;

    public enum Mode {
        PLAYER_ONLY,
        BATCH_ALL_CARS
    }
}
