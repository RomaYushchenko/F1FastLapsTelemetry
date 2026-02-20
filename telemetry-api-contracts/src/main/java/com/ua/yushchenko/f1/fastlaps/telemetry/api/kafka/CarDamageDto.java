package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Car damage payload (topic telemetry.carDamage).
 * Tyre wear in percentage per wheel. F1 25 CarDamageData m_tyresWear[4] order: RL, RR, FL, FR.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarDamageDto {

    /** Tyre wear percentage – Front Left */
    private Float tyresWearFL;
    /** Tyre wear percentage – Front Right */
    private Float tyresWearFR;
    /** Tyre wear percentage – Rear Left */
    private Float tyresWearRL;
    /** Tyre wear percentage – Rear Right */
    private Float tyresWearRR;
}
