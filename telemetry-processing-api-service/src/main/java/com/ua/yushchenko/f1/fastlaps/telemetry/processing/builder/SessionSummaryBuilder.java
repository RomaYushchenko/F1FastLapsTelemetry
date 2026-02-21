package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;

/**
 * Builds SessionSummary (assembly only). Empty summary when session exists but no laps aggregated yet.
 * See: implementation_phases.md Phase 4.1.
 */
public final class SessionSummaryBuilder {

    private SessionSummaryBuilder() {
    }

    /**
     * Empty summary for session+car with zero laps (lastUpdatedAt set by entity @PrePersist/@PreUpdate).
     */
    public static SessionSummary empty(long sessionUid, short carIndex) {
        return SessionSummary.builder()
                .sessionUid(sessionUid)
                .carIndex(carIndex)
                .totalLaps((short) 0)
                .build();
    }
}
