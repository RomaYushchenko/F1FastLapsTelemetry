package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for car telemetry (topic telemetry.carTelemetry). Includes envelope metadata and a {@link CarTelemetryDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CarTelemetryEvent extends AbstractTelemetryEvent {

    private CarTelemetryDto payload;

    @Override
    public CarTelemetryDto getPayload() {
        return payload;
    }
}
