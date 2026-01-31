package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import lombok.Data;

/**
 * Runtime state for a single lap (in-memory).
 * Tracks sector completion and lap progress.
 * See: implementation_steps_plan.md § Етап 6.1.
 */
@Data
public class LapRuntimeState {

    private final long sessionUid;
    private final short carIndex;
    
    private short currentLapNumber;
    private Integer currentLapTimeMs;
    
    // Sector times (null until completed)
    private Integer sector1TimeMs;
    private Integer sector2TimeMs;
    private Integer sector3TimeMs;
    
    private boolean isInvalid;
    private short penaltiesSeconds;
    
    // Current sector (0 = not started, 1-3 = sector number)
    private int currentSector;

    public LapRuntimeState(long sessionUid, short carIndex) {
        this.sessionUid = sessionUid;
        this.carIndex = carIndex;
        this.currentLapNumber = 0;
        this.currentSector = 0;
        this.isInvalid = false;
        this.penaltiesSeconds = 0;
    }

    /**
     * Check if lap is complete (all 3 sectors finished).
     */
    public boolean isComplete() {
        return sector1TimeMs != null && sector2TimeMs != null && sector3TimeMs != null;
    }

    /**
     * Reset for next lap.
     */
    public void reset(short newLapNumber) {
        this.currentLapNumber = newLapNumber;
        this.currentLapTimeMs = null;
        this.sector1TimeMs = null;
        this.sector2TimeMs = null;
        this.sector3TimeMs = null;
        this.currentSector = 0;
        this.isInvalid = false;
        this.penaltiesSeconds = 0;
    }
}
