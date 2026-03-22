package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.AbstractTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryBatchEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.CarTelemetryIdempotency;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.CarTelemetryProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.carTelemetry topic. Thin: deserialize, ensureSession, shouldProcess, idempotency, then CarTelemetryProcessor.
 * Handles legacy {@link CarTelemetryEvent} and batched {@link CarTelemetryBatchEvent}.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarTelemetryConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final CarTelemetryProcessor carTelemetryProcessor;

    @KafkaListener(
            topics = "telemetry.carTelemetry",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(AbstractTelemetryEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-ct-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        if (event instanceof CarTelemetryBatchEvent batch) {
            consumeBatch(batch, acknowledgment);
        } else if (event instanceof CarTelemetryEvent single) {
            consumeSingle(single, acknowledgment);
        } else {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-ct-unknown");
            try {
                log.warn("Skipping car telemetry record: unsupported type {}", event.getClass().getName());
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
        }
    }

    private void consumeSingle(CarTelemetryEvent event, Acknowledgment acknowledgment) {
        String traceId = "kafka-ct-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = (short) event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);
            lifecycleService.setPlayerCarIndex(sessionUid, carIndex);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping car telemetry packet: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Skipping car telemetry packet: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            float sessionTime = event.getSessionTime() != 0 ? (float) event.getSessionTime() : 0f;
            carTelemetryProcessor.process(sessionUid, carIndex, frameId, event.getPayload(), sessionTime);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car telemetry", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private void consumeBatch(CarTelemetryBatchEvent event, Acknowledgment acknowledgment) {
        String traceId = "kafka-ct-batch-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();

            lifecycleService.ensureSessionActive(sessionUid);
            lifecycleService.setPlayerCarIndex(sessionUid, (short) event.getCarIndex());
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping car telemetry batch: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, CarTelemetryIdempotency.BATCH_FRAME_CAR_INDEX)) {
                log.debug("Skipping car telemetry batch: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            float sessionTime = event.getSessionTime() != 0 ? (float) event.getSessionTime() : 0f;
            carTelemetryProcessor.processBatch(sessionUid, frameId, sessionTime, event.getSamples());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car telemetry batch", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
