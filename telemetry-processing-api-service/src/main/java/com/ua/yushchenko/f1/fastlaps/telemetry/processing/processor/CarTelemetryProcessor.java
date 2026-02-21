package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.RawTelemetryWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes car telemetry: watermark update, merge snapshot, pedal trace (with lap attribution).
 * Called from CarTelemetryConsumer after ensureSession, shouldProcess, idempotency.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarTelemetryProcessor {

    private static final float LAP_DISTANCE_DROP_THRESHOLD_M = 500f;

    private final SessionStateManager stateManager;
    private final RawTelemetryWriter rawTelemetryWriter;

    private final Map<String, LapTraceState> lapTraceStateBySessionCar = new ConcurrentHashMap<>();

    private static final class LapTraceState {
        float lastLapDistanceM;
        short lastLapNumber;
    }

    public void process(long sessionUid, short carIndex, int frameId, CarTelemetryDto telemetry, float sessionTime) {
        log.debug("process: sessionUid={}, carIndex={}, frameId={}", sessionUid, carIndex, frameId);
        SessionRuntimeState state = stateManager.getOrCreate(sessionUid);
        int currentWatermark = state.getWatermark(carIndex);
        if (frameId < currentWatermark) {
            log.debug("Out-of-order telemetry packet ignored: sessionUid={}, frame={}, watermark={}",
                    sessionUid, frameId, currentWatermark);
            return;
        }
        state.updateWatermark(carIndex, frameId);

        SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(carIndex);
        if (snapshot == null) {
            snapshot = new SessionRuntimeState.CarSnapshot();
        }
        snapshot.setSpeedKph(telemetry.getSpeedKph());
        snapshot.setGear(telemetry.getGear());
        snapshot.setEngineRpm(telemetry.getEngineRpm());
        snapshot.setThrottle(telemetry.getThrottle());
        snapshot.setBrake(telemetry.getBrake());
        snapshot.setTimestamp(Instant.now());
        state.updateSnapshot(carIndex, snapshot);

        if (state.isActive()) {
            Short lapNum = resolveLapNumberForTrace(sessionUid, carIndex, snapshot);
            rawTelemetryWriter.write(
                    Instant.now(),
                    sessionUid,
                    frameId,
                    carIndex,
                    telemetry.getSpeedKph(),
                    telemetry.getThrottle(),
                    telemetry.getBrake(),
                    sessionTime != 0 ? sessionTime : null,
                    lapNum,
                    snapshot.getLapDistanceM()
            );
        }
    }

    /**
     * Resolve lap number for pedal trace using lap distance wrap-around so telemetry is attributed
     * to the correct lap even when CarTelemetry is processed before LapData (different Kafka topics).
     */
    private Short resolveLapNumberForTrace(long sessionUid, short carIndex, SessionRuntimeState.CarSnapshot snapshot) {
        String key = sessionUid + "-" + carIndex;
        LapTraceState traceState = lapTraceStateBySessionCar.computeIfAbsent(key, k -> new LapTraceState());

        Float lapDistanceM = snapshot.getLapDistanceM();
        Integer snapshotLap = snapshot.getCurrentLap();

        short lapNum;
        if (lapDistanceM != null) {
            float distance = lapDistanceM;
            if (traceState.lastLapDistanceM > LAP_DISTANCE_DROP_THRESHOLD_M && distance < traceState.lastLapDistanceM - LAP_DISTANCE_DROP_THRESHOLD_M) {
                short inferredFromWrap = (short) (traceState.lastLapNumber + 1);
                if (snapshotLap != null && snapshotLap > 0 && inferredFromWrap > snapshotLap) {
                    lapNum = snapshotLap.shortValue();
                } else {
                    lapNum = inferredFromWrap;
                }
            } else if (snapshotLap != null && snapshotLap > 0) {
                lapNum = snapshotLap.shortValue();
            } else {
                lapNum = traceState.lastLapNumber > 0 ? traceState.lastLapNumber : 1;
            }
            traceState.lastLapDistanceM = distance;
            traceState.lastLapNumber = lapNum;
        } else {
            lapNum = snapshotLap != null && snapshotLap > 0
                    ? snapshotLap.shortValue()
                    : (traceState.lastLapNumber > 0 ? traceState.lastLapNumber : 1);
            traceState.lastLapNumber = lapNum;
        }
        return lapNum;
    }
}
