package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Car status payload (topic telemetry.carStatus).
 * See: kafka_contracts_f_1_telemetry.md § 5.4.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarStatusDto {

    private Integer tractionControl;
    private Integer abs;
    private Float fuelInTank;
    private Integer fuelMix;
    private Boolean drsAllowed;
    private Integer tyresCompound;
    private Float ersStoreEnergy;
}
