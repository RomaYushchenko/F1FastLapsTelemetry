package com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;

/**
 * Builds TyreWearPerLap from TyreWearSnapshot (assembly only).
 * See: implementation_phases.md Phase 4.1.
 */
public final class TyreWearPerLapBuilder {

    private TyreWearPerLapBuilder() {
    }

    /**
     * Build TyreWearPerLap entity from snapshot and optional compound for the given lap.
     */
    public static TyreWearPerLap fromSnapshot(
            long sessionUid, short carIndex, short lapNumber,
            TyreWearSnapshot snapshot, Short compound) {
        if (snapshot == null) {
            return null;
        }
        return TyreWearPerLap.builder()
                .sessionUid(sessionUid)
                .carIndex(carIndex)
                .lapNumber(lapNumber)
                .wearFL(snapshot.getWearFL())
                .wearFR(snapshot.getWearFR())
                .wearRL(snapshot.getWearRL())
                .wearRR(snapshot.getWearRR())
                .compound(compound)
                .build();
    }
}
