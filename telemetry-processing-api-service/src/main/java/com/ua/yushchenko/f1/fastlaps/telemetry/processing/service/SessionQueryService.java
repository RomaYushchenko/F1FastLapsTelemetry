package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read operations for sessions: list, get by id, get active.
 * Uses SessionResolveService, SessionRepository, SessionStateManager, SessionMapper.
 * See: implementation_phases.md Phase 2.1.
 */
@Service
@RequiredArgsConstructor
public class SessionQueryService {

    private final SessionRepository sessionRepository;
    private final SessionStateManager stateManager;
    private final SessionMapper sessionMapper;
    private final SessionResolveService sessionResolveService;

    /**
     * List sessions with pagination (most recent first).
     */
    public List<SessionDto> listSessions(int offset, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        int page = offset / size;
        Pageable pageable = PageRequest.of(page, size);
        return sessionRepository.findAllByOrderByCreatedAtDesc(pageable).stream()
                .map(s -> sessionMapper.toDto(s, stateManager.get(s.getSessionUid())))
                .collect(Collectors.toList());
    }

    /**
     * Get session by public id or session UID.
     *
     * @throws SessionNotFoundException if id is blank or session not found
     */
    public SessionDto getSession(String id) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty() || "active".equalsIgnoreCase(trimmedId)) {
            throw new SessionNotFoundException(
                    trimmedId.isEmpty() ? "Session id is required" : "Use GET /api/sessions/active for active session");
        }
        Session session = sessionResolveService.getSessionByPublicIdOrUid(trimmedId);
        return sessionMapper.toDto(session, stateManager.get(session.getSessionUid()));
    }

    /**
     * Get current active session, if any.
     */
    public Optional<SessionDto> getActiveSession() {
        return stateManager.getAllActive().values().stream()
                .findFirst()
                .map(state -> sessionRepository.findById(state.getSessionUID()))
                .filter(Optional::isPresent)
                .map(opt -> {
                    Session s = opt.get();
                    return sessionMapper.toDto(s, stateManager.get(s.getSessionUid()));
                });
    }
}
