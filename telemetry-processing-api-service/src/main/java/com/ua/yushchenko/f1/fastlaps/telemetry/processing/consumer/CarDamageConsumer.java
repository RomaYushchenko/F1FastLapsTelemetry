package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarDamageEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.CarDamageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.carDamage topic. Thin: deserialize, ensureSession, shouldProcess, idempotency, then CarDamageProcessor.
 * See: implementation_phases.md Phase 5.1; kafka_contracts_f_1_telemetry.md § 9.2.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CarDamageConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final CarDamageProcessor carDamageProcessor;

    @KafkaListener(
            topics = "telemetry.carDamage",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(CarDamageEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-cd-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-cd-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = (short) event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);
            lifecycleService.setPlayerCarIndex(sessionUid, carIndex);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping car damage packet: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Skipping car damage packet: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            carDamageProcessor.process(sessionUid, carIndex, event.getPayload());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing car damage", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
