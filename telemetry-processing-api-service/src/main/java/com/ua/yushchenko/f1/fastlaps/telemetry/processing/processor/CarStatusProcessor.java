package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.CarStatusRawWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Processes car status: watermark update, snapshot DRS, persist to car_status_raw when active.
 * Called from CarStatusConsumer after ensureSession, shouldProcess, idempotency.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarStatusProcessor {

    private final SessionStateManager stateManager;
    private final CarStatusRawWriter carStatusRawWriter;

    public void process(long sessionUid, short carIndex, int frameId, CarStatusDto status, float sessionTime) {
        SessionRuntimeState state = stateManager.getOrCreate(sessionUid);
        int currentWatermark = state.getWatermark(carIndex);
        if (frameId < currentWatermark) {
            log.debug("Out-of-order status packet ignored: sessionUid={}, frame={}, watermark={}",
                    sessionUid, frameId, currentWatermark);
            return;
        }
        state.updateWatermark(carIndex, frameId);

        log.debug("Car status: sessionUid={}, carIndex={}, fuel={}, drsAllowed={}",
                sessionUid, carIndex, status.getFuelInTank(), status.getDrsAllowed());

        SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(carIndex);
        if (snapshot == null) {
            snapshot = new SessionRuntimeState.CarSnapshot();
            state.updateSnapshot(carIndex, snapshot);
        }
        snapshot.setDrs(Boolean.TRUE.equals(status.getDrsAllowed()));

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
