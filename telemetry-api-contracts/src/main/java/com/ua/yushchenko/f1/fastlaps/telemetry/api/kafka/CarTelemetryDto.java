package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Car telemetry payload (topic telemetry.carTelemetry).
 * Maps F1 25 CarTelemetryData; see .github/docs/F1 25 Telemetry Output Structures.txt and kafka_contracts_f_1_telemetry.md § 5.3.
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

    /** F1 25 CarTelemetryData m_clutch — amount of clutch applied (0 to 100). */
    private Integer clutch;
    /** F1 25 CarTelemetryData m_revLightsPercent — rev lights indicator (percentage). */
    private Integer revLightsPercent;
    /** F1 25 CarTelemetryData m_revLightsBitValue — rev lights (bit 0 = leftmost LED, bit 14 = rightmost). */
    private Integer revLightsBitValue;
    /** F1 25 CarTelemetryData m_brakesTemperature[4] — celsius, order RL, RR, FL, FR. */
    private int[] brakesTemperature;
    /** F1 25 CarTelemetryData m_tyresSurfaceTemperature[4] — celsius, order RL, RR, FL, FR. */
    private int[] tyresSurfaceTemperature;
    /** F1 25 CarTelemetryData m_tyresInnerTemperature[4] — celsius, order RL, RR, FL, FR. */
    private int[] tyresInnerTemperature;
    /** F1 25 CarTelemetryData m_engineTemperature — engine temperature (celsius). */
    private Integer engineTemperature;
    /** F1 25 CarTelemetryData m_tyresPressure[4] — PSI, order RL, RR, FL, FR. */
    private float[] tyresPressure;
    /** F1 25 CarTelemetryData m_surfaceType[4] — driving surface per appendices, order RL, RR, FL, FR. */
    private int[] surfaceType;
}
