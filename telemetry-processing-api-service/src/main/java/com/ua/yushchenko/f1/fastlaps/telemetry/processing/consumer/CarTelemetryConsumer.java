package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.KafkaEnvelope;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer for telemetry.carTelemetry topic.
 * Handles high-frequency telemetry data (speed, RPM, throttle, brake, etc.).
 * See: implementation_steps_plan.md § Етап 5.6.
 *
 * <p>Uses {@link KafkaEnvelope}{@code <CarTelemetryDto>} for compatibility with the current
 * telemetry-api-contracts. When CarTelemetryEvent and RawTelemetryWriter are available,
 * switch to CarTelemetryEvent and add pedal trace persistence with lap attribution
 * (lap distance wrap-around).
 *
 * <p><b>Lap attribution (P1):</b> Lap and telemetry topics are processed independently.
 * When inferring lap number from distance wrap (e.g. lastLapNumber + 1), always use
 * {@link #resolveLapForPedalTrace(int, Integer)} with {@code snapshot.getCurrentLap()}
 * so that if LapDataConsumer already advanced the lap to N+1, we do not double-increment
 * to N+2 (which would corrupt pedal-trace lap attribution).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarTelemetryConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final SessionStateManager stateManager;

    @KafkaListener(
            topics = "telemetry.carTelemetry",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(KafkaEnvelope<CarTelemetryDto> envelope, Acknowledgment acknowledgment) {
        try {
            long sessionUid = envelope.getSessionUID();
            int frameId = envelope.getFrameIdentifier();
            short packetId = (short) envelope.getPacketId().ordinal();
            short carIndex = (short) envelope.getCarIndex();

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

            CarTelemetryDto telemetry = envelope.getPayload();

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

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car telemetry", e);
            throw e;
        }
    }

    /**
     * Resolves lap number for pedal trace when lap is inferred from distance wrap.
     * Prevents double-increment: LapDataConsumer may have already set snapshot to N+1
     * before we process the telemetry packet with the wrap, so we must not add +1 again.
     *
     * @param inferredFromWrap lap inferred from wrap (e.g. lastLapNumber + 1)
     * @param snapshotLap      current lap from snapshot (updated by LapDataConsumer), may be null
     * @return lap number to use: snapshotLap if it is already >= inferredFromWrap, else inferredFromWrap
     */
    static short resolveLapForPedalTrace(int inferredFromWrap, Integer snapshotLap) {
        if (snapshotLap != null && snapshotLap > 0 && snapshotLap >= inferredFromWrap) {
            return snapshotLap.shortValue();
        }
        return (short) inferredFromWrap;
    }
}
