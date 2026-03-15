package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.ParticipantsEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.config.TraceIdFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.participants topic. Updates session runtime state with race number and driver name per car (for Live Track Map).
 * No idempotency: last participants payload wins.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipantsConsumer {

    private final SessionStateManager sessionStateManager;

    @KafkaListener(
            topics = "telemetry.participants",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ParticipantsEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            MDC.put(TraceIdFilter.MDC_TRACE_ID, "kafka-participants-null");
            try {
                log.warn("Skipping record: deserialization failed (e.g. old format without @type)");
                acknowledgment.acknowledge();
            } finally {
                MDC.clear();
            }
            return;
        }
        String traceId = "kafka-participants-" + event.getSessionUID();
        MDC.put(TraceIdFilter.MDC_TRACE_ID, traceId);
        try {
            long sessionUid = event.getSessionUID();
            var payload = event.getPayload();
            if (payload == null || payload.getParticipants() == null) {
                log.debug("Skipping participants: sessionUid={}, payload empty", sessionUid);
                acknowledgment.acknowledge();
                return;
            }
            var state = sessionStateManager.getOrCreate(sessionUid);
            state.setParticipants(payload.getParticipants());
            log.debug("Updated participants: sessionUid={}, numParticipants={}", sessionUid, payload.getParticipants().size());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing participants", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
