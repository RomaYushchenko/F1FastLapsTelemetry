package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.LapDataProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.lap topic. Thin: deserialize, ensureSession, shouldProcess, idempotency, then LapDataProcessor.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LapDataConsumer {

    private static final Logger INBOUND_LOG = LoggerFactory.getLogger("inbound-events");

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;
    private final LapDataProcessor lapDataProcessor;

    @KafkaListener(
            topics = "telemetry.lap",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(LapDataEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-lap-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-lap-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            INBOUND_LOG.debug("Received event: topic=telemetry.lap, sessionUid={}, frame={}",
                    event.getSessionUID(), event.getFrameIdentifier());
            long sessionUid = event.getSessionUID();
            int frameId = event.getFrameIdentifier();
            short packetId = (short) event.getPacketId().ordinal();
            short carIndex = (short) event.getCarIndex();

            lifecycleService.ensureSessionActive(sessionUid);
            lifecycleService.setPlayerCarIndex(sessionUid, carIndex);
            if (!lifecycleService.shouldProcessPacket(sessionUid)) {
                log.debug("Skipping lap data packet: sessionUid={}, frame={}, reason=shouldNotProcess", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Skipping lap data packet: sessionUid={}, frame={}, reason=duplicate", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            lapDataProcessor.process(sessionUid, carIndex, frameId, event.getPayload());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing lap data", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
