package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for car damage (topic telemetry.carDamage). Includes envelope metadata and a {@link CarDamageDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CarDamageEvent extends AbstractTelemetryEvent {

    private CarDamageDto payload;

    @Override
    public CarDamageDto getPayload() {
        return payload;
    }
}
