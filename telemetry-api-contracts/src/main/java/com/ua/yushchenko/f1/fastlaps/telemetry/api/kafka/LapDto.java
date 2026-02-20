package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lap data payload (topic telemetry.lap).
 * See: kafka_contracts_f_1_telemetry.md § 5.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LapDto {

    private int lapNumber;
    private float lapDistance;
    /** Official last lap time (m_lastLapTimeInMS). Present on first packet of new lap. */
    private Integer lastLapTimeMs;
    private Integer currentLapTimeMs;
    /** Sector 1 time in ms from game (m_sector1TimeMinutesPart * 60000 + m_sector1TimeMSPart). */
    private Integer sector1TimeMs;
    /** Sector 2 time in ms from game (m_sector2TimeMinutesPart * 60000 + m_sector2TimeMSPart). */
    private Integer sector2TimeMs;
    private Integer sector;
    private boolean isInvalid;
    private Integer penaltiesSeconds;
}
