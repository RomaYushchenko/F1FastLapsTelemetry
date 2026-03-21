package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Batched car telemetry for one UDP frame: all grid slots in a single Kafka record.
 * Reduces Kafka write amplification vs publishing one {@link CarTelemetryEvent} per car.
 * Topic remains {@code telemetry.carTelemetry}. Superclass {@code carIndex} is the human player slot (from header).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CarTelemetryBatchEvent extends AbstractTelemetryEvent {

    private List<CarTelemetrySlotDto> samples;

    @Override
    public Object getPayload() {
        return samples;
    }
}
