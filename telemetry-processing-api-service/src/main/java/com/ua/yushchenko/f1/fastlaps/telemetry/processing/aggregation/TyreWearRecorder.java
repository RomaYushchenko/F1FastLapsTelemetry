package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder.TyreWearPerLapBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.LastTyreCompoundState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records last known tyre wear for a lap when the lap is finalized.
 * Called from LapAggregator after saving the lap.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TyreWearRecorder {

    private final TyreWearState tyreWearState;
    private final LastTyreCompoundState lastTyreCompoundState;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;

    /**
     * Record current tyre wear for the given lap (session+car+lapNumber).
     * Uses last snapshot from TyreWearState and last compound from LastTyreCompoundState; no-op if no snapshot available.
     */
    @Transactional
    public void recordForLap(long sessionUid, short carIndex, short lapNumber) {
        TyreWearSnapshot snapshot = tyreWearState.get(sessionUid, carIndex);
        Short compound = lastTyreCompoundState.get(sessionUid, carIndex);
        TyreWearPerLap entity = TyreWearPerLapBuilder.fromSnapshot(sessionUid, carIndex, lapNumber, snapshot, compound);
        if (entity == null) {
            log.debug("No tyre wear snapshot for sessionUid={}, carIndex={}, lap={}", sessionUid, carIndex, lapNumber);
            return;
        }
        tyreWearPerLapRepository.save(entity);
        log.debug("Recorded tyre wear for sessionUid={}, carIndex={}, lap={}", sessionUid, carIndex, lapNumber);
    }
}
