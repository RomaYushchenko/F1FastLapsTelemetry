package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.ErsDeployMode;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.CarStatusRawWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.LastTyreCompoundState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Processes car status: watermark update, snapshot ERS and drsAllowed (zone); persist to car_status_raw when active.
 * Snapshot DRS (wing open/closed) is set in {@link CarTelemetryProcessor} from Car Telemetry m_drs; this processor sets only drsAllowed (zone active) and ERS.
 * ERS_MAX_ENERGY_J (4 MJ) from F1 25 spec / regulations; used to compute ersEnergyPercent.
 * Called from CarStatusConsumer after ensureSession, shouldProcess, idempotency.
 * See: implementation_phases.md Phase 5.1, plan 12.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarStatusProcessor {

    /**
     * ERS maximum energy store in Joules (F1 2022+ regulations: 4 MJ per lap).
     * Used to compute ersEnergyPercent = 100 * ersStoreEnergy / ERS_MAX_ENERGY_J.
     * Source: F1 25 Telemetry Output Structures (m_ersStoreEnergy in Joules); max from regulations.
     */
    private static final float ERS_MAX_ENERGY_J = 4_000_000f;

    private final SessionStateManager stateManager;
    private final CarStatusRawWriter carStatusRawWriter;
    private final LastTyreCompoundState lastTyreCompoundState;

    public void process(long sessionUid, short carIndex, int frameId, CarStatusDto status, float sessionTime) {
        log.debug("process: sessionUid={}, carIndex={}, frameId={}", sessionUid, carIndex, frameId);
        SessionRuntimeState state = stateManager.getOrCreate(sessionUid);
        int currentWatermark = state.getStatusWatermark(carIndex);
        if (frameId < currentWatermark) {
            log.debug("Out-of-order status packet ignored: sessionUid={}, frame={}, watermark={}",
                    sessionUid, frameId, currentWatermark);
            return;
        }
        state.updateStatusWatermark(carIndex, frameId);

        log.debug("Car status: sessionUid={}, carIndex={}, fuel={}, drsAllowed={}",
                sessionUid, carIndex, status.getFuelInTank(), status.getDrsAllowed());

        SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(carIndex);
        if (snapshot == null) {
            snapshot = new SessionRuntimeState.CarSnapshot();
            state.updateSnapshot(carIndex, snapshot);
        }
        snapshot.setDrsAllowed(Boolean.TRUE.equals(status.getDrsAllowed()));

        if (status.getFuelInTank() != null && status.getFuelCapacity() != null && status.getFuelCapacity() > 0) {
            float percent = 100f * status.getFuelInTank() / status.getFuelCapacity();
            snapshot.setFuelRemainingPercent((int) Math.max(0, Math.min(100, Math.round(percent))));
        } else {
            snapshot.setFuelRemainingPercent(null);
        }

        if (status.getErsStoreEnergy() != null && ERS_MAX_ENERGY_J > 0) {
            float percent = 100f * status.getErsStoreEnergy() / ERS_MAX_ENERGY_J;
            snapshot.setErsEnergyPercent((int) Math.max(0, Math.min(100, Math.round(percent))));
        } else {
            snapshot.setErsEnergyPercent(null);
        }
        ErsDeployMode ersMode = ErsDeployMode.fromCode(status.getErsDeployMode());
        snapshot.setErsDeployActive(ersMode.isDeployActive());
        snapshot.setErsDeployMode(status.getErsDeployMode());

        if (status.getTyresCompound() != null) {
            lastTyreCompoundState.update(sessionUid, carIndex, status.getTyresCompound());
        }
        if (status.getVisualTyreCompound() != null) {
            snapshot.setVisualTyreCompound(status.getVisualTyreCompound());
        }

        if (state.isActive()) {
            carStatusRawWriter.write(
                    Instant.now(),
                    sessionUid,
                    frameId,
                    carIndex,
                    status,
                    sessionTime
            );
        }
    }
}
