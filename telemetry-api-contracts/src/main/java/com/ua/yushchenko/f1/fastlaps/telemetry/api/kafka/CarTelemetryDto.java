package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Car telemetry payload (topic telemetry.carTelemetry).
 * See: kafka_contracts_f_1_telemetry.md § 5.3.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarTelemetryDto {

    private Integer speedKph;
    private Float throttle;
    private Float brake;
    private Float steer;
    private Integer gear;
    private Integer engineRpm;
    private Integer drs;
}
