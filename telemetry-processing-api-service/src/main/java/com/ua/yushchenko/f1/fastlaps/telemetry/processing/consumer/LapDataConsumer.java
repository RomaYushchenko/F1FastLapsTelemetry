package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation.LapAggregator;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.lap topic.
 * Handles lap data updates and sector completion.
 * See: implementation_steps_plan.md § Етап 5.5.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LapDataConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final SessionStateManager stateManager;
    private final LapAggregator lapAggregator;

    @KafkaListener(
            topics = "telemetry.lap",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(LapDataEvent event, Acknowledgment acknowledgment) {
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
                log.debug("Out-of-order lap packet ignored: sessionUid={}, frame={}, watermark={}",
                        sessionUid, frameId, currentWatermark);
                acknowledgment.acknowledge();
                return;
            }

            // Update watermark
            state.updateWatermark(carIndex, frameId);

            LapDto lap = event.getPayload();
            log.debug("Lap data: sessionUid={}, carIndex={}, lap={}, sector={}, lapTime={}ms",
                    sessionUid, carIndex, lap.getLapNumber(), lap.getSector(), lap.getCurrentLapTimeMs());

            // Pass to LapAggregator for sector/lap finalization
            lapAggregator.processLapData(sessionUid, carIndex, lap);

            // Update live snapshot with current lap/sector for WebSocket.
            // Create snapshot if absent so lap/sector are preserved when telemetry packets merge into it later.
            SessionRuntimeState.CarSnapshot snapshot = state.getSnapshot(carIndex);
            if (snapshot == null) {
                snapshot = new SessionRuntimeState.CarSnapshot();
                state.updateSnapshot(carIndex, snapshot);
            }
            snapshot.setCurrentLap(lap.getLapNumber());
            snapshot.setCurrentSector(lap.getSector() != null ? lap.getSector() : 0);
            snapshot.setLapDistanceM(lap.getLapDistance());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing lap data", e);
            throw e;
        }
    }
}
