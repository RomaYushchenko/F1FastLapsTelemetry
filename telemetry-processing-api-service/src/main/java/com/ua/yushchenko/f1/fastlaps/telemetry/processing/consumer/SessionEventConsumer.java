package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor.SessionEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.session topic. Thin: deserialize, idempotency, then SessionEventProcessor.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionEventProcessor sessionEventProcessor;

    @KafkaListener(
            topics = "telemetry.session",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(SessionLifecycleEvent event, Acknowledgment acknowledgment) {
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

            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Duplicate session event, skipping: sessionUid={}, frame={}", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            SessionEventDto payload = event.getPayload();
            sessionEventProcessor.process(sessionUid, payload);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing session event", e);
            throw e;
        }
    }
}
