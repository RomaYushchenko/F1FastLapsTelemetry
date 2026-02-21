package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionQueryService {

    private final SessionRepository sessionRepository;
    private final LapRepository lapRepository;
    private final SessionFinishingPositionRepository finishingPositionRepository;
    private final SessionStateManager stateManager;
    private final SessionMapper sessionMapper;
    private final SessionResolveService sessionResolveService;

    /**
     * When session has no player_car_index set (e.g. created before we added the column),
     * infer from existing laps so the UI can request the correct car's data.
     */
    private Integer inferPlayerCarIndexFromData(Long sessionUid) {
        if (sessionUid == null) {
            return null;
        }
        return lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(sessionUid).stream()
                .findFirst()
                .map(lap -> lap.getCarIndex() != null ? lap.getCarIndex().intValue() : null)
                .orElse(null);
    }

    private void applyInferredPlayerCarIndex(Session session, SessionDto dto) {
        if (dto != null && dto.getPlayerCarIndex() == null) {
            Integer inferred = inferPlayerCarIndexFromData(session.getSessionUid());
            if (inferred != null) {
                dto.setPlayerCarIndex(inferred);
                log.debug("Inferred playerCarIndex={} for sessionUID={} from laps", inferred, session.getSessionUid());
            }
        }
    }

    /**
     * Set finishing position on DTO from session_finishing_positions (player car). Plan: 03-session-page.md Etap 3.
     */
    private void applyFinishingPosition(Session session, SessionDto dto) {
        if (dto == null || session.getSessionUid() == null) {
            return;
        }
        Short carIndex = session.getPlayerCarIndex();
        if (carIndex == null) {
            Integer inferred = inferPlayerCarIndexFromData(session.getSessionUid());
            if (inferred == null) {
                return;
            }
            carIndex = inferred.shortValue();
        }
        finishingPositionRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex)
                .ifPresent(fp -> dto.setFinishingPosition(fp.getFinishingPosition()));
    }

    /**
     * List sessions with pagination (most recent first).
     */
    public List<SessionDto> listSessions(int offset, int limit) {
        log.debug("listSessions: offset={}, limit={}", offset, limit);
        int size = Math.max(1, Math.min(limit, 100));
        int page = offset / size;
        Pageable pageable = PageRequest.of(page, size);
        List<SessionDto> result = sessionRepository.findAllByOrderByCreatedAtDesc(pageable).stream()
                .map(s -> {
                    SessionDto dto = sessionMapper.toDto(s, stateManager.get(s.getSessionUid()));
                    applyInferredPlayerCarIndex(s, dto);
                    applyFinishingPosition(s, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        log.debug("listSessions: returning {} sessions", result.size());
        return result;
    }

    /**
     * Get session by public id or session UID.
     *
     * @throws SessionNotFoundException if id is blank or session not found
     */
    public SessionDto getSession(String id) {
        log.debug("getSession: id={}", id);
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty() || "active".equalsIgnoreCase(trimmedId)) {
            log.warn("Invalid session id: empty or 'active'");
            throw new SessionNotFoundException(
                    trimmedId.isEmpty() ? "Session id is required" : "Use GET /api/sessions/active for active session");
        }
        Session session = sessionResolveService.getSessionByPublicIdOrUid(trimmedId);
        SessionDto dto = sessionMapper.toDto(session, stateManager.get(session.getSessionUid()));
        applyInferredPlayerCarIndex(session, dto);
        applyFinishingPosition(session, dto);
        log.debug("getSession: resolved id={}", SessionMapper.toPublicIdString(session));
        return dto;
    }

    /**
     * Get current active session, if any.
     * Returns only sessions in ACTIVE state (user is driving); ignores ENDING (session ended, flushing).
     * This ensures live view shows the current qualification/race, not the previous session.
     */
    public Optional<SessionDto> getActiveSession() {
        log.debug("getActiveSession");
        Optional<SessionDto> result = stateManager.getAllActive().values().stream()
                .filter(state -> state.isActive())
                .findFirst()
                .map(state -> sessionRepository.findById(state.getSessionUID()))
                .filter(Optional::isPresent)
                .map(opt -> {
                    Session s = opt.get();
                    SessionDto dto = sessionMapper.toDto(s, stateManager.get(s.getSessionUid()));
                    applyInferredPlayerCarIndex(s, dto);
                    applyFinishingPosition(s, dto);
                    return dto;
                });
        log.debug("getActiveSession: {}", result.isPresent() ? "present" : "empty");
        return result;
    }

    /**
     * Resolve topic id (public_id or session_uid string) for a session by its UID.
     * Used by LiveDataBroadcaster so topic matches REST/WebSocket client id.
     */
    public Optional<String> getTopicIdForSession(Long sessionUid) {
        log.debug("getTopicIdForSession: sessionUid={}", sessionUid);
        if (sessionUid == null) {
            log.debug("getTopicIdForSession: sessionUid is null, returning empty");
            return Optional.empty();
        }
        Optional<String> topicId = sessionRepository.findById(sessionUid)
                .map(SessionMapper::toPublicIdString);
        log.debug("getTopicIdForSession: topicId={}", topicId.orElse("empty"));
        return topicId;
    }
}
