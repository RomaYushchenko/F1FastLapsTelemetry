package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Car status payload (topic telemetry.carStatus).
 * Maps F1 25 CarStatusData; see .github/docs/F1 25 Telemetry Output Structures.txt and kafka_contracts_f_1_telemetry.md § 5.4.
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
    /** F1 25 CarStatusData m_actualTyreCompound. */
    private Integer tyresCompound;
    /** F1 25 CarStatusData m_tyresAgeLaps. */
    private Integer tyresAgeLaps;
    private Float ersStoreEnergy;

    /** F1 25 CarStatusData m_frontBrakeBias — front brake bias (percentage). */
    private Integer frontBrakeBias;
    /** F1 25 CarStatusData m_pitLimiterStatus — 0 = off, 1 = on. */
    private Integer pitLimiterStatus;
    /** F1 25 CarStatusData m_fuelCapacity. */
    private Float fuelCapacity;
    /** F1 25 CarStatusData m_fuelRemainingLaps — fuel remaining in terms of laps (MFD value). */
    private Float fuelRemainingLaps;
    /** F1 25 CarStatusData m_maxRPM — rev limiter point. */
    private Integer maxRpm;
    /** F1 25 CarStatusData m_idleRPM. */
    private Integer idleRpm;
    /** F1 25 CarStatusData m_maxGears. */
    private Integer maxGears;
    /** F1 25 CarStatusData m_drsActivationDistance — metres; 0 = DRS not available. */
    private Integer drsActivationDistance;
    /** F1 25 CarStatusData m_visualTyreCompound. */
    private Integer visualTyreCompound;
    /** F1 25 CarStatusData m_vehicleFIAFlags — -1 = invalid/unknown, 0 = none, 1 = green, 2 = blue, 3 = yellow. */
    private Integer vehicleFiaFlags;
    /** F1 25 CarStatusData m_enginePowerICE — engine power output (W). */
    private Float enginePowerIce;
    /** F1 25 CarStatusData m_enginePowerMGUK — MGU-K power output (W). */
    private Float enginePowerMguk;
    /** F1 25 CarStatusData m_ersDeployMode — 0 = none, 1 = medium, 2 = hotlap, 3 = overtake. */
    private Integer ersDeployMode;
    /** F1 25 CarStatusData m_ersHarvestedThisLapMGUK. */
    private Float ersHarvestedThisLapMguk;
    /** F1 25 CarStatusData m_ersHarvestedThisLapMGUH. */
    private Float ersHarvestedThisLapMguh;
    /** F1 25 CarStatusData m_ersDeployedThisLap. */
    private Float ersDeployedThisLap;
    /** F1 25 CarStatusData m_networkPaused — car paused in network game. */
    private Integer networkPaused;
}
