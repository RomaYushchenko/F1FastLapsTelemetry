package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarStatusEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.CarStatusProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.carStatus topic. Thin: deserialize, ensureSession, shouldProcess, idempotency, then CarStatusProcessor.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarStatusConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final CarStatusProcessor carStatusProcessor;

    @KafkaListener(
            topics = "telemetry.carStatus",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CarStatusEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-cs-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-cs-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = (short) event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping car status packet: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Skipping car status packet: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            float sessionTime = event.getSessionTime();
            carStatusProcessor.process(sessionUid, carIndex, frameId, event.getPayload(), sessionTime);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car status", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
