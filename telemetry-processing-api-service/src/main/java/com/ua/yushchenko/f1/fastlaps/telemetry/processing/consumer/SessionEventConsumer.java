package com.ua.yushchenko.f1.fastlaps.telemetry.processing.consumer;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionLifecycleEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.idempotency.IdempotencyService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.EndReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for telemetry.session topic.
 * Handles session lifecycle events: SSTA (started), SEND (ended).
 * See: implementation_steps_plan.md § Етап 5.4.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    private final IdempotencyService idempotencyService;
    private final SessionLifecycleService lifecycleService;

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

            // Idempotency check
            if (!idempotencyService.markAsProcessed(sessionUid, frameId, packetId, carIndex)) {
                log.debug("Duplicate session event, skipping: sessionUid={}, frame={}", sessionUid, frameId);
                acknowledgment.acknowledge();
                return;
            }

            SessionEventDto payload = event.getPayload();
            EventCode eventCode = payload.getEventCode();

            // Handle event (no per-packet info logging; exceptional cases logged in lifecycle or at debug)
            switch (eventCode) {
                case SSTA -> lifecycleService.onSessionStarted(sessionUid, payload);
                case SEND -> lifecycleService.onSessionEnded(sessionUid, payload, EndReason.EVENT_SEND);
                case SESSION_INFO -> lifecycleService.onSessionInfo(sessionUid, payload);
                case SESSION_TIMEOUT -> lifecycleService.onSessionTimeout(sessionUid);
                case FLBK -> log.debug("Flashback event received for session {}, ignoring (MVP)", sessionUid);
                default -> log.warn("Unknown event code: {}", eventCode);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing session event", e);
            // Do not acknowledge - will be redelivered
            throw e;
        }
    }
}
