package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusDto;
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

    @KafkaListener(
            topics = "telemetry.carStatus",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(KafkaEnvelope<CarStatusDto> envelope, Acknowledgment acknowledgment) {
        try {
            long sessionUid = envelope.getSessionUID();
            int frameId = envelope.getFrameIdentifier();
            short packetId = (short) envelope.getPacketId().ordinal();
            short carIndex = (short) envelope.getCarIndex();

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

            CarStatusDto status = envelope.getPayload();
            log.debug("Car status: sessionUid={}, carIndex={}, fuel={}, drsAllowed={}",
                    sessionUid, carIndex, status.getFuelInTank(), status.getDrsAllowed());

            // TODO (Етап 6): Pass to RawTelemetryWriter for batch insert into car_status_raw

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car status", e);
            throw e;
        }
    }
}
