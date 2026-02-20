package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for lap data (topic telemetry.lap). Includes envelope metadata and a {@link LapDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LapDataEvent extends AbstractTelemetryEvent {

    private LapDto payload;

    @Override
    public LapDto getPayload() {
        return payload;
    }
}
