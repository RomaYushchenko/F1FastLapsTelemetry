package com.ua.yushchenko.f1.fastlaps.telemetry.processing.processor;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.EventCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.lifecycle.SessionLifecycleService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.EndReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processes session lifecycle events (SSTA, SEND, SESSION_INFO, SESSION_TIMEOUT).
 * Called from SessionEventConsumer after idempotency.
 * See: implementation_phases.md Phase 5.1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventProcessor {

    private final SessionLifecycleService lifecycleService;

    /**
     * Process session event by event code. No per-packet info logging; exceptional cases logged in lifecycle or at debug.
     */
    public void process(long sessionUid, SessionEventDto payload) {
        log.debug("process: sessionUid={}, eventCode={}", sessionUid, payload != null ? payload.getEventCode() : null);
        if (payload == null) {
            log.warn("Session event payload is null, skipping: sessionUid={}", sessionUid);
            return;
        }
        EventCode eventCode = payload.getEventCode();
        switch (eventCode) {
            case SSTA -> lifecycleService.onSessionStarted(sessionUid, payload);
            case SEND -> lifecycleService.onSessionEnded(sessionUid, payload, EndReason.EVENT_SEND);
            case SESSION_INFO -> lifecycleService.onSessionInfo(sessionUid, payload);
            case SESSION_TIMEOUT -> lifecycleService.onSessionTimeout(sessionUid);
            case FLBK -> log.debug("Flashback event received for session {}, ignoring (MVP)", sessionUid);
            default -> log.warn("Unknown event code: {}", eventCode);
        }
    }
}
