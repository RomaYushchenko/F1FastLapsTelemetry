package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full F1 25 PacketSessionData payload (topic telemetry.sessionData).
 * Maps packet-specific data after header; see .github/docs/F1 25 Telemetry Output Structures.txt (PacketSessionData, 724 bytes).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDataDto {

    private Integer weather;
    private Integer trackTemperature;
    private Integer airTemperature;
    private Integer totalLaps;
    private Integer trackLength;
    private Integer sessionType;
    private Integer trackId;
    private Integer formula;
    private Integer sessionTimeLeft;
    private Integer sessionDuration;
    private Integer pitSpeedLimit;
    private Integer gamePaused;
    private Integer isSpectating;
    private Integer spectatorCarIndex;
    private Integer sliProNativeSupport;
    private Integer numMarshalZones;
    /** MarshalZone[21]: zone start (0..1). */
    private float[] marshalZoneStarts;
    /** MarshalZone[21]: zone flag -1/0/1/2/3. */
    private int[] marshalZoneFlags;
    private Integer safetyCarStatus;
    private Integer networkGame;
    private Integer numWeatherForecastSamples;
    /** WeatherForecastSample[64]: sessionType per sample. */
    private int[] weatherForecastSessionType;
    private int[] weatherForecastTimeOffset;
    private int[] weatherForecastWeather;
    private int[] weatherForecastTrackTemperature;
    private int[] weatherForecastTrackTemperatureChange;
    private int[] weatherForecastAirTemperature;
    private int[] weatherForecastAirTemperatureChange;
    private int[] weatherForecastRainPercentage;
    private Integer forecastAccuracy;
    private Integer aiDifficulty;
    private Long seasonLinkIdentifier;
    private Long weekendLinkIdentifier;
    private Long sessionLinkIdentifier;
    private Integer pitStopWindowIdealLap;
    private Integer pitStopWindowLatestLap;
    private Integer pitStopRejoinPosition;
    private Integer steeringAssist;
    private Integer brakingAssist;
    private Integer gearboxAssist;
    private Integer pitAssist;
    private Integer pitReleaseAssist;
    private Integer ersAssist;
    private Integer drsAssist;
    private Integer dynamicRacingLine;
    private Integer dynamicRacingLineType;
    private Integer gameMode;
    private Integer ruleSet;
    private Long timeOfDay;
    private Integer sessionLength;
    private Integer speedUnitsLeadPlayer;
    private Integer temperatureUnitsLeadPlayer;
    private Integer speedUnitsSecondaryPlayer;
    private Integer temperatureUnitsSecondaryPlayer;
    private Integer numSafetyCarPeriods;
    private Integer numVirtualSafetyCarPeriods;
    private Integer numRedFlagPeriods;
    private Integer equalCarPerformance;
    private Integer recoveryMode;
    private Integer flashbackLimit;
    private Integer surfaceType;
    private Integer lowFuelMode;
    private Integer raceStarts;
    private Integer tyreTemperature;
    private Integer pitLaneTyreSim;
    private Integer carDamage;
    private Integer carDamageRate;
    private Integer collisions;
    private Integer collisionsOffForFirstLapOnly;
    private Integer mpUnsafePitRelease;
    private Integer mpOffForGriefing;
    private Integer cornerCuttingStringency;
    private Integer parcFermeRules;
    private Integer pitStopExperience;
    private Integer safetyCar;
    private Integer safetyCarExperience;
    private Integer formationLap;
    private Integer formationLapExperience;
    private Integer redFlags;
    private Integer affectsLicenceLevelSolo;
    private Integer affectsLicenceLevelMP;
    private Integer numSessionsInWeekend;
    /** m_weekendStructure[12]. */
    private int[] weekendStructure;
    private Float sector2LapDistanceStart;
    private Float sector3LapDistanceStart;
}
