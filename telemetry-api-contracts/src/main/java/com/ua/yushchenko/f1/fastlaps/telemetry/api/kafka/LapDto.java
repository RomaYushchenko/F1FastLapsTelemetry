package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lap data payload (topic telemetry.lap).
 * Maps F1 25 LapData; see .github/docs/F1 25 Telemetry Output Structures.txt and kafka_contracts_f_1_telemetry.md § 5.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LapDto {

    private int lapNumber;
    private float lapDistance;
    /** F1 25 LapData m_lastLapTimeInMS. Present on first packet of new lap. */
    private Integer lastLapTimeMs;
    /** F1 25 LapData m_currentLapTimeInMS. */
    private Integer currentLapTimeMs;
    /** Sector 1 time in ms (m_sector1TimeMinutesPart * 60000 + m_sector1TimeMSPart). */
    private Integer sector1TimeMs;
    /** Sector 2 time in ms (m_sector2TimeMinutesPart * 60000 + m_sector2TimeMSPart). */
    private Integer sector2TimeMs;
    /** F1 25 LapData m_sector — 0 = sector1, 1 = sector2, 2 = sector3. */
    private Integer sector;
    private boolean isInvalid;
    /** F1 25 LapData m_penalties — accumulated time penalties in seconds. */
    private Integer penaltiesSeconds;

    /** F1 25 LapData m_deltaToCarInFront (minutesPart * 60000 + msPart). */
    private Integer deltaToCarInFrontMs;
    /** F1 25 LapData m_deltaToRaceLeader (minutesPart * 60000 + msPart). */
    private Integer deltaToRaceLeaderMs;
    /** F1 25 LapData m_totalDistance — total distance travelled in session (metres). */
    private Float totalDistance;
    /** F1 25 LapData m_safetyCarDelta — delta in seconds for safety car. */
    private Float safetyCarDelta;
    /** F1 25 LapData m_carPosition — car race position. */
    private Integer carPosition;
    /** F1 25 LapData m_pitStatus — 0 = none, 1 = pitting, 2 = in pit area. */
    private Integer pitStatus;
    /** F1 25 LapData m_numPitStops. */
    private Integer numPitStops;
    /** F1 25 LapData m_totalWarnings. */
    private Integer totalWarnings;
    /** F1 25 LapData m_cornerCuttingWarnings. */
    private Integer cornerCuttingWarnings;
    /** F1 25 LapData m_numUnservedDriveThroughPens. */
    private Integer numUnservedDriveThroughPens;
    /** F1 25 LapData m_numUnservedStopGoPens. */
    private Integer numUnservedStopGoPens;
    /** F1 25 LapData m_gridPosition. */
    private Integer gridPosition;
    /** F1 25 LapData m_driverStatus — 0 = in garage, 1 = flying lap, 2 = in lap, 3 = out lap, 4 = on track. */
    private Integer driverStatus;
    /** F1 25 LapData m_resultStatus — 0 = invalid, 1 = inactive, 2 = active, 3 = finished, etc. */
    private Integer resultStatus;
    /** F1 25 LapData m_pitLaneTimerActive — 0 = inactive, 1 = active. */
    private Integer pitLaneTimerActive;
    /** F1 25 LapData m_pitLaneTimeInLaneInMS. */
    private Integer pitLaneTimeInLaneInMs;
    /** F1 25 LapData m_pitStopTimerInMS. */
    private Integer pitStopTimerInMs;
    /** F1 25 LapData m_pitStopShouldServePen. */
    private Integer pitStopShouldServePen;
    /** F1 25 LapData m_speedTrapFastestSpeed — km/h. */
    private Float speedTrapFastestSpeed;
    /** F1 25 LapData m_speedTrapFastestLap — lap number, 255 = not set. */
    private Integer speedTrapFastestLap;
}
