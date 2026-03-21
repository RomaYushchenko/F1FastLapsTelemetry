package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetrySlotDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.DrsState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.RawTelemetryWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes car telemetry: watermark update, merge snapshot (including DRS wing state), pedal trace (with lap attribution).
 * Snapshot DRS (wing open/closed) is set from telemetry m_drs (0=off, 1=on); DRS allowed (zone) is set from Car Status in CarStatusProcessor.
 * Supports batched samples (all cars in one frame) with a single {@code saveAll} to reduce DB load.
 * See: implementation_phases.md Phase 5.1, plan 12, plan 14.
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
        Instant ts = Instant.now();
        CarTelemetryRaw row = processOneSample(sessionUid, carIndex, frameId, telemetry, sessionTime, ts);
        if (row != null) {
            rawTelemetryWriter.save(row);
        }
    }

    /**
     * Process all cars from one {@link com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent}.
     * Uses one shared {@code Instant} for PK {@code ts} (unique per car via {@code car_index}).
     */
    public void processBatch(long sessionUid, int frameId, float sessionTime, List<CarTelemetrySlotDto> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        Instant ts = Instant.now();
        List<CarTelemetryRaw> rows = new ArrayList<>(samples.size());
        for (CarTelemetrySlotDto slot : samples) {
            if (slot == null || slot.getTelemetry() == null) {
                continue;
            }
            short carIndex = (short) slot.getCarIndex();
            CarTelemetryRaw row = processOneSample(sessionUid, carIndex, frameId, slot.getTelemetry(), sessionTime, ts);
            if (row != null) {
                rows.add(row);
            }
        }
        if (!rows.isEmpty()) {
            rawTelemetryWriter.saveAll(rows);
        }
    }

    private CarTelemetryRaw processOneSample(
            long sessionUid,
            short carIndex,
            int frameId,
            CarTelemetryDto telemetry,
            float sessionTime,
            Instant ts
    ) {
        log.debug("processOneSample: sessionUid={}, carIndex={}, frameId={}", sessionUid, carIndex, frameId);
        SessionRuntimeState state = stateManager.getOrCreate(sessionUid);
        int currentWatermark = state.getTelemetryWatermark(carIndex);
        if (frameId < currentWatermark) {
            log.debug("Out-of-order telemetry packet ignored: sessionUid={}, frame={}, watermark={}",
                    sessionUid, frameId, currentWatermark);
            return null;
        }
        state.updateTelemetryWatermark(carIndex, frameId);

        SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(carIndex);
        if (snapshot == null) {
            snapshot = new SessionRuntimeState.CarSnapshot();
        }
        snapshot.setSpeedKph(telemetry.getSpeedKph());
        snapshot.setGear(telemetry.getGear());
        snapshot.setEngineRpm(telemetry.getEngineRpm());
        snapshot.setThrottle(telemetry.getThrottle());
        snapshot.setBrake(telemetry.getBrake());
        snapshot.setDrs(DrsState.fromCode(telemetry.getDrs()) == DrsState.ON);
        int[] tyreTemps = telemetry.getTyresSurfaceTemperature();
        snapshot.setTyresSurfaceTempC(tyreTemps != null ? tyreTemps.clone() : null);
        snapshot.setTimestamp(Instant.now());
        snapshot.setSessionTimeSeconds(sessionTime != 0 ? sessionTime : null);
        state.updateSnapshot(carIndex, snapshot);

        if (!state.isActive()) {
            return null;
        }
        Short lapNum = resolveLapNumberForTrace(sessionUid, carIndex, snapshot);
        return rawTelemetryWriter.buildRow(
                ts,
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
