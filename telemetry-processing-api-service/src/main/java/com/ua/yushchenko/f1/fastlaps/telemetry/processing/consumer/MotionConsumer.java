package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.MotionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.MotionRawWriter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackLayoutRecordingService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer for telemetry.motion topic. Persists motion to motion_raw and updates live positions (B9).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MotionConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final MotionRawWriter motionRawWriter;
    private final SessionStateManager sessionStateManager;
    private final TrackLayoutRecordingService trackLayoutRecordingService;

    @KafkaListener(
            topics = "telemetry.motion",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(MotionEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-motion-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-motion-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = (short) event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping motion packet: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Skipping motion packet: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            var payload = event.getPayload();
            if (payload != null) {
                motionRawWriter.write(
                        Instant.now(),
                        sessionUid,
                        frameId,
                        carIndex,
                        payload.getGForceLateral(),
                        payload.getYaw(),
                        payload.getWorldPositionX(),
                        payload.getWorldPositionZ()
                );
                // Update live positions for B9 (Live Track Map)
                var state = sessionStateManager.get(sessionUid);
                if (state != null) {
                    state.updatePosition(carIndex, payload.getWorldPositionX(), payload.getWorldPositionZ());
                    float lapDistance = state.getLatestLapDistance(carIndex);
                    trackLayoutRecordingService.onMotionFrame(
                            sessionUid,
                            carIndex,
                            payload.getWorldPositionX(),
                            payload.getWorldPositionY(),
                            payload.getWorldPositionZ(),
                            lapDistance
                    );
                }
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing motion", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
