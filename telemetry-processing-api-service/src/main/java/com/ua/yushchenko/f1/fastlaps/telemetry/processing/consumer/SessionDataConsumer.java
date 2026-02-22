package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionDataEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.sessionData topic (full PacketSessionData, 724 bytes).
 * Ensures session exists and updates metadata so practice/qualifying/race show correctly
 * when the game sends full session packet before SSTA or lap data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionDataConsumer {

    private final SessionLifecycleService lifecycleService;

    @KafkaListener(
            topics = "telemetry.sessionData",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(SessionDataEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-sd-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-sd-" + event.getSessionUID() + "-" + event.getFrameIdentifier();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            lifecycleService.onSessionData(event.getSessionUID(), event.getPayload());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing session data", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
