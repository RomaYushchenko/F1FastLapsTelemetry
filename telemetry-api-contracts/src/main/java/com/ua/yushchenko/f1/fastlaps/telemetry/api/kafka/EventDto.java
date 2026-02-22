package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * F1 25 Packet Event payload (topic telemetry.event).
 * Event code string (4 chars) plus optional detail fields depending on event type.
 * All reason/type fields are integer codes; use reference enums (RetirementReason, DrsDisabledReason,
 * SafetyCarType, SafetyCarEventType) in processing for display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {

    /** Event string code, e.g. "DRSD", "SCAR", "RTMT", "FTLP". */
    private String eventCode;

    /** FastestLap: vehicle index. */
    private Integer vehicleIdx;
    /** FastestLap: lap time in seconds. */
    private Float lapTime;

    /** Retirement: reason code (0–10). */
    private Integer retirementReason;

    /** DRSDisabled: reason code (0–3). */
    private Integer drsDisabledReason;

    /** SafetyCar: safetyCarType (0–3), eventType (0–3). */
    private Integer safetyCarType;
    private Integer safetyCarEventType;

    /** Penalty: vehicleIdx, otherVehicleIdx, penaltyType, infringementType, time, lapNum, placesGained. */
    private Integer otherVehicleIdx;
    private Integer penaltyType;
    private Integer infringementType;
    private Integer penaltyTime;
    private Integer penaltyLapNum;
    private Integer placesGained;

    /** SpeedTrap: vehicleIdx, speed (kph), isOverallFastestInSession, isDriverFastestInSession, etc. */
    private Float speedTrapSpeedKph;
    private Integer isOverallFastestInSession;
    private Integer isDriverFastestInSession;
    private Integer fastestVehicleIdxInSession;
    private Float fastestSpeedInSession;

    /** StartLights: numLights. */
    private Integer numLights;

    /** DriveThroughPenaltyServed / StopGoPenaltyServed: vehicleIdx. */
    /** StopGoPenaltyServed: stopTime in seconds. */
    private Float stopTimeSeconds;

    /** Flashback: frameIdentifier, sessionTime. */
    private Long flashbackFrameIdentifier;
    private Float flashbackSessionTime;

    /** Overtake: overtakingVehicleIdx, beingOvertakenVehicleIdx. */
    private Integer overtakingVehicleIdx;
    private Integer beingOvertakenVehicleIdx;

    /** Collision: vehicle1Idx, vehicle2Idx. */
    private Integer vehicle1Idx;
    private Integer vehicle2Idx;
}
