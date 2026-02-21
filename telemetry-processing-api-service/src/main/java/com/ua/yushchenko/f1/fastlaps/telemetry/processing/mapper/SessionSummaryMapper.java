package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import org.springframework.stereotype.Component;

/**
 * Maps SessionSummary entity to REST DTO.
 * See: implementation_phases.md Phase 1.1.
 */
@Component
public class SessionSummaryMapper {

    public SessionSummaryDto toDto(SessionSummary summary) {
        if (summary == null) {
            return null;
        }
        return SessionSummaryDto.builder()
                .totalLaps(summary.getTotalLaps() != null ? summary.getTotalLaps().intValue() : null)
                .bestLapTimeMs(summary.getBestLapTimeMs())
                .bestLapNumber(summary.getBestLapNumber() != null ? summary.getBestLapNumber().intValue() : null)
                .bestSector1Ms(summary.getBestSector1Ms())
                .bestSector2Ms(summary.getBestSector2Ms())
                .bestSector3Ms(summary.getBestSector3Ms())
                .build();
    }

    /**
     * Empty summary when session exists but no laps have been aggregated yet.
     */
    public static SessionSummaryDto emptySummaryDto() {
        return SessionSummaryDto.builder()
                .totalLaps(0)
                .bestLapTimeMs(null)
                .bestLapNumber(null)
                .bestSector1Ms(null)
                .bestSector2Ms(null)
                .bestSector3Ms(null)
                .build();
    }
}
