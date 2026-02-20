package com.ua.yushchenko.f1.fastlaps.telemetry.processing.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Last known tyre wear (percentage per wheel) for a session+car.
 * Updated by CarDamageConsumer from telemetry.carDamage; read by TyreWearRecorder when finalizing a lap.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TyreWearSnapshot {

    private Float wearFL;
    private Float wearFR;
    private Float wearRL;
    private Float wearRR;
}
