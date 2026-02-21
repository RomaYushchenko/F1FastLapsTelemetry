package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation.LapAggregator;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processes lap data: watermark update, LapAggregator, live snapshot (lap/sector/distance).
 * Called from LapDataConsumer after ensureSession, shouldProcess, idempotency.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LapDataProcessor {

    private final SessionStateManager stateManager;
    private final LapAggregator lapAggregator;

    /**
     * Process lap data packet. Updates watermark, runs sector/lap logic, updates WebSocket snapshot.
     */
    public void process(long sessionUid, short carIndex, int frameId, LapDto lap) {
        log.debug("process: sessionUid={}, carIndex={}, frameId={}", sessionUid, carIndex, frameId);
        SessionRuntimeState state = stateManager.getOrCreate(sessionUid);
        int currentWatermark = state.getLapWatermark(carIndex);
        if (frameId < currentWatermark) {
            log.debug("Out-of-order lap packet ignored: sessionUid={}, frame={}, watermark={}",
                    sessionUid, frameId, currentWatermark);
            return;
        }
        state.updateLapWatermark(carIndex, frameId);

        log.debug("Lap data: sessionUid={}, carIndex={}, lap={}, sector={}, lapTime={}ms",
                sessionUid, carIndex, lap.getLapNumber(), lap.getSector(), lap.getCurrentLapTimeMs());

        lapAggregator.processLapData(sessionUid, carIndex, lap);

        SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(carIndex);
        if (snapshot == null) {
            snapshot = new SessionRuntimeState.CarSnapshot();
            state.updateSnapshot(carIndex, snapshot);
        }
        snapshot.setCurrentLap(lap.getLapNumber());
        snapshot.setCurrentSector(lap.getSector() != null ? lap.getSector() : 0);
        snapshot.setLapDistanceM(lap.getLapDistance());
    }
}
