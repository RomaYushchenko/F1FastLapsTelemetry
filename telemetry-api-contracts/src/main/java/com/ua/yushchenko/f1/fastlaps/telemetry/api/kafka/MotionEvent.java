package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Kafka event for motion data (topic telemetry.motion). Includes envelope metadata and a {@link MotionDto} payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MotionEvent extends AbstractTelemetryEvent {

    private MotionDto payload;

    @Override
    public MotionDto getPayload() {
        return payload;
    }
}
