package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes session events (FTLP, PENA, SCAR, etc.) to session_events table.
 * Block E — Session events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventWriter {

    private final SessionEventRepository sessionEventRepository;

    /**
     * Persist session event and return the saved entity (for WebSocket push).
     */
    public SessionEvent write(Long sessionUid, int frameId, Short lap, String eventCode, Short carIndex, String detailJson) {
        log.debug("write: sessionUid={}, frameId={}, lap={}, eventCode={}", sessionUid, frameId, lap, eventCode);
        SessionEvent entity = SessionEvent.builder()
                .sessionUid(sessionUid)
                .frameId(frameId)
                .lap(lap)
                .eventCode(eventCode)
                .carIndex(carIndex)
                .detail(detailJson)
                .build();
        return sessionEventRepository.save(entity);
    }
}
