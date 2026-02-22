package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.EventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.event topic. Thin: deserialize, ensureSession, shouldProcess,
 * idempotency, then EventProcessor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final EventProcessor eventProcessor;

    @KafkaListener(
            topics = "telemetry.event",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(EventEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-ev-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-ev-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = 0;

            lifecycleService.ensureSessionActive(sessionUid);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping event packet: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Skipping event packet: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            eventProcessor.process(sessionUid, frameId, event.getPayload());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing event", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
