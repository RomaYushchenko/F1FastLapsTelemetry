package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralized session resolution by public id or session UID.
 * Normalizes input, delegates to repository, throws SessionNotFoundException when not found.
 * Use in REST controllers, WebSocket controller and later in query services.
 * See: implementation_phases.md Phase 1.3.
 */
@Slf4j
@Service
public class SessionResolveService {

    private final SessionRepository sessionRepository;

    public SessionResolveService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Resolve session by public id (UUID string) or session_uid (numeric string).
     *
     * @param id trimmed non-empty string (UUID or session_uid)
     * @return Session entity
     * @throws SessionNotFoundException if id is blank or session not found
     */
    public Session getSessionByPublicIdOrUid(String id) {
        log.debug("getSessionByPublicIdOrUid: id={}", id);
        String trimmed = id != null ? id.trim() : "";
        if (trimmed.isEmpty()) {
            log.warn("Session id is required");
            throw new SessionNotFoundException("Session id is required");
        }
        Session session = sessionRepository.findByPublicIdOrSessionUid(trimmed)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + trimmed));
        log.debug("getSessionByPublicIdOrUid: resolved sessionUid={}, publicId={}",
                session.getSessionUid(), SessionMapper.toPublicIdString(session));
        return session;
    }
}
