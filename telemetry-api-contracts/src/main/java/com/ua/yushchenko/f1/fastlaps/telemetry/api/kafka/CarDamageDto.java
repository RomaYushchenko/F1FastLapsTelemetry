package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Car damage payload (topic telemetry.carDamage).
 * Maps F1 25 CarDamageData; see .github/docs/F1 25 Telemetry Output Structures.txt and kafka_contracts_f_1_telemetry.md § 5.5.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarDamageDto {

    /** F1 25 CarDamageData m_tyresWear[4] — order RL, RR, FL, FR. */
    private Float tyresWearFL;
    private Float tyresWearFR;
    private Float tyresWearRL;
    private Float tyresWearRR;

    /** F1 25 CarDamageData m_tyresDamage[4] — percentage, order RL, RR, FL, FR. */
    private int[] tyresDamage;
    /** F1 25 CarDamageData m_brakesDamage[4] — percentage, order RL, RR, FL, FR. */
    private int[] brakesDamage;
    /** F1 25 CarDamageData m_tyreBlisters[4] — percentage, order RL, RR, FL, FR. */
    private int[] tyreBlisters;

    /** F1 25 CarDamageData m_frontLeftWingDamage (%). */
    private Integer frontLeftWingDamage;
    /** F1 25 CarDamageData m_frontRightWingDamage (%). */
    private Integer frontRightWingDamage;
    /** F1 25 CarDamageData m_rearWingDamage (%). */
    private Integer rearWingDamage;
    /** F1 25 CarDamageData m_floorDamage (%). */
    private Integer floorDamage;
    /** F1 25 CarDamageData m_diffuserDamage (%). */
    private Integer diffuserDamage;
    /** F1 25 CarDamageData m_sidepodDamage (%). */
    private Integer sidepodDamage;
    /** F1 25 CarDamageData m_drsFault — 0 = OK, 1 = fault. */
    private Integer drsFault;
    /** F1 25 CarDamageData m_ersFault — 0 = OK, 1 = fault. */
    private Integer ersFault;
    /** F1 25 CarDamageData m_gearBoxDamage (%). */
    private Integer gearBoxDamage;
    /** F1 25 CarDamageData m_engineDamage (%). */
    private Integer engineDamage;
    /** F1 25 CarDamageData m_engineMGUHWear (%). */
    private Integer engineMguhWear;
    /** F1 25 CarDamageData m_engineESWear (%). */
    private Integer engineEsWear;
    /** F1 25 CarDamageData m_engineCEWear (%). */
    private Integer engineCeWear;
    /** F1 25 CarDamageData m_engineICEWear (%). */
    private Integer engineIceWear;
    /** F1 25 CarDamageData m_engineMGUKWear (%). */
    private Integer engineMgukWear;
    /** F1 25 CarDamageData m_engineTCWear (%). */
    private Integer engineTcWear;
    /** F1 25 CarDamageData m_engineBlown — 0 = OK, 1 = fault. */
    private Integer engineBlown;
    /** F1 25 CarDamageData m_engineSeized — 0 = OK, 1 = fault. */
    private Integer engineSeized;
}
