package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.RawTelemetryWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka consumer for telemetry.carTelemetry topic.
 * Handles high-frequency telemetry data (speed, RPM, throttle, brake, etc.).
 * See: implementation_steps_plan.md § Етап 5.6.
 *
 * <p>Consumes {@link CarTelemetryEvent} (polymorphic {@code @type} in JSON). Producer must
 * send event types so Jackson can deserialize to the correct subclass.
 *
 * <p>Lap attribution: LapData and CarTelemetry are on different Kafka topics, so processing
 * order is not guaranteed. We infer lap number from lap distance wrap-around so that
 * telemetry for lap 2+ is stored under the correct lap (see {@link #resolveLapNumberForTrace}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarTelemetryConsumer {

    private static final float LAP_DISTANCE_DROP_THRESHOLD_M = 500f;

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final SessionStateManager stateManager;
    private final RawTelemetryWriter rawTelemetryWriter;

    /** Per session+car: last lap distance and lap number, used to detect new lap from distance reset. */
    private final Map<String, LapTraceState> lapTraceStateBySessionCar = new ConcurrentHashMap<>();

    private static final class LapTraceState {
        float lastLapDistanceM;
        short lastLapNumber;
    }

    @KafkaListener(
            topics = "telemetry.carTelemetry",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CarTelemetryEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
            acknowledgment.acknowledge();
            return;
        }
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = (short) event.getCarIndex();

            // Implicit session start: if no SSTA was received, create session on first data packet
            lifecycleService.ensureSessionActive(sessionUid);

            // Check if session should process packets
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                acknowledgment.acknowledge();
                return;
            }

            // Idempotency check
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                acknowledgment.acknowledge();
                return;
            }

            // Watermark check
            SessionRuntimeState state = stateManager.getOrCreate(sessionUid);
            int currentWatermark = state.getWatermark(carIndex);
            if (frameId < currentWatermark) {
                log.debug("Out-of-order telemetry packet ignored: sessionUid={}, frame={}, watermark={}",
                        sessionUid, frameId, currentWatermark);
                acknowledgment.acknowledge();
                return;
            }

            // Update watermark
            state.updateWatermark(carIndex, frameId);

            CarTelemetryDto telemetry = event.getPayload();

            // Update snapshot for WebSocket live feed.
            // Important: reuse existing snapshot (if any) so that fields coming
            // from other, lower-frequency packets (DRS, lap/sector, etc.) are
            // preserved between telemetry updates.
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
                        event.getSessionTime() != 0 ? (float) event.getSessionTime() : null,
                        lapNum,
                        snapshot.getLapDistanceM()
                );
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car telemetry", e);
            throw e;
        }
    }

    /**
     * Resolve lap number for storing pedal trace. Uses lap distance wrap-around so that
     * telemetry is attributed to the correct lap even when CarTelemetry is processed
     * before LapData (different Kafka topics).
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
                lapNum = (short) (traceState.lastLapNumber + 1);
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
