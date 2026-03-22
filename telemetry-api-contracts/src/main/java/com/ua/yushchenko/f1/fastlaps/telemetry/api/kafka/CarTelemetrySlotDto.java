package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One car slot inside {@link CarTelemetryBatchEvent} (UDP packet 6 carries up to 22 slots).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarTelemetrySlotDto {

    /** Grid slot index 0–21. */
    private int carIndex;

    private CarTelemetryDto telemetry;
}
