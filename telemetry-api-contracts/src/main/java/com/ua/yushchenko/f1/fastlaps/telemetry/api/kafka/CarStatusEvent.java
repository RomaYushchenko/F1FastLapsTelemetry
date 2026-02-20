package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for car status (topic telemetry.carStatus). Includes envelope metadata and a {@link CarStatusDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CarStatusEvent extends AbstractTelemetryEvent {

    private CarStatusDto payload;

    @Override
    public CarStatusDto getPayload() {
        return payload;
    }
}
