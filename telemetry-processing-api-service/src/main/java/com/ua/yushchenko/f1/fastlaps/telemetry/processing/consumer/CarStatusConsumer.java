package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.CarStatusRawWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer for telemetry.carStatus topic.
 * Handles car status data (fuel, tires, ERS, etc.).
 * See: implementation_steps_plan.md § Етап 5.7.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarStatusConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final SessionStateManager stateManager;
    private final CarStatusRawWriter carStatusRawWriter;

    @KafkaListener(
            topics = "telemetry.carStatus",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CarStatusEvent event, Acknowledgment acknowledgment) {
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            log.info("Received car status: sessionUID={}, frame={}, carIndex={}", sessionUid, frameId, event.getCarIndex());
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
                log.debug("Out-of-order status packet ignored: sessionUid={}, frame={}, watermark={}",
                        sessionUid, frameId, currentWatermark);
                acknowledgment.acknowledge();
                return;
            }

            // Update watermark
            state.updateWatermark(carIndex, frameId);

            CarStatusDto status = event.getPayload();
            log.debug("Car status: sessionUid={}, carIndex={}, fuel={}, drsAllowed={}",
                    sessionUid, carIndex, status.getFuelInTank(), status.getDrsAllowed());

            // Update live snapshot with DRS for WebSocket (same carIndex as telemetry snapshot).
            // Create snapshot if absent so DRS is preserved when telemetry packets merge into it later.
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
                        event.getSessionTime()
                );
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car status", e);
            throw e;
        }
    }
}
