package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;

import java.time.Instant;

/**
 * Builds Lap entity from final lap values (assembly only; sector/lap time logic stays in LapAggregator).
 * See: implementation_phases.md Phase 4.1.
 */
public final class LapBuilder {

    private LapBuilder() {
    }

    /**
     * Build Lap entity from finalized lap data.
     *
     * @param positionAtLapStart race position at the start of this lap (from LapData carPosition); may be null.
     */
    public static Lap build(
            long sessionUid,
            short carIndex,
            short lapNumber,
            int lapTimeMs,
            int sector1TimeMs,
            int sector2TimeMs,
            int sector3TimeMs,
            boolean isInvalid,
            short penaltiesSeconds,
            Integer positionAtLapStart,
            Instant endedAt
    ) {
        return Lap.builder()
                .sessionUid(sessionUid)
                .carIndex(carIndex)
                .lapNumber(lapNumber)
                .lapTimeMs(lapTimeMs)
                .sector1TimeMs(sector1TimeMs)
                .sector2TimeMs(sector2TimeMs)
                .sector3TimeMs(sector3TimeMs)
                .isInvalid(isInvalid)
                .penaltiesSeconds(penaltiesSeconds)
                .positionAtLapStart(positionAtLapStart)
                .endedAt(endedAt)
                .build();
    }
}
