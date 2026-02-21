package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearSnapshot;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.TyreWearState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processes car damage (tyre wear): updates in-memory TyreWearState per session+car.
 * TyreWearRecorder persists when lap is finalized.
 * Called from CarDamageConsumer after ensureSession, shouldProcess.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarDamageProcessor {

    private final TyreWearState tyreWearState;

    public void process(long sessionUid, short carIndex, CarDamageDto dto) {
        log.debug("process: sessionUid={}, carIndex={}", sessionUid, carIndex);
        if (dto == null) {
            log.debug("Car damage dto is null, skipping");
            return;
        }
        TyreWearSnapshot snapshot = new TyreWearSnapshot(
                dto.getTyresWearFL(),
                dto.getTyresWearFR(),
                dto.getTyresWearRL(),
                dto.getTyresWearRR()
        );
        tyreWearState.update(sessionUid, carIndex, snapshot);
        log.debug("Updated tyre wear state: sessionUid={}, carIndex={}", sessionUid, carIndex);
    }
}
