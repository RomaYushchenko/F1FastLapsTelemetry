package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lap data payload (topic telemetry.lap).
 * See: kafka_contracts_f_1_telemetry.md § 5.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LapDto {

    private int lapNumber;
    private float lapDistance;
    private Integer currentLapTimeMs;
    private Integer sector;
    private boolean isInvalid;
    private Integer penaltiesSeconds;
}
