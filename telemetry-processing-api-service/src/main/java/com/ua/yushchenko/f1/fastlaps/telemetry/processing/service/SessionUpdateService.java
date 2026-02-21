package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates session attributes (e.g. display name).
 * Plan: 03-session-page.md Etap 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionUpdateService {

    private final SessionResolveService sessionResolveService;
    private final SessionRepository sessionRepository;
    private final SessionStateManager stateManager;
    private final SessionMapper sessionMapper;

    /**
     * Update session display name by id (public id or session UID string).
     *
     * @param id                session public id or session_uid (trimmed, non-empty)
     * @param sessionDisplayName new display name (validated: not blank, max 64)
     * @return updated SessionDto
     * @throws SessionNotFoundException if session not found
     */
    @Transactional
    public SessionDto updateDisplayName(String id, String sessionDisplayName) {
        log.debug("updateDisplayName: id={}, sessionDisplayName length={}", id, sessionDisplayName != null ? sessionDisplayName.length() : 0);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(id);
        session.setSessionDisplayName(sessionDisplayName.trim());
        sessionRepository.save(session);
        SessionDto dto = sessionMapper.toDto(session, stateManager.get(session.getSessionUid()));
        log.debug("updateDisplayName: updated session id={}", SessionMapper.toPublicIdString(session));
        return dto;
    }
}
