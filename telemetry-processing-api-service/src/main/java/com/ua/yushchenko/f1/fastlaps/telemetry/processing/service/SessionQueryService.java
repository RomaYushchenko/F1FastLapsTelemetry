package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionParticipantDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.spec.SessionListSpecification;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
    private final SessionSummaryRepository sessionSummaryRepository;
    private final SessionFinishingPositionRepository finishingPositionRepository;
    private final SessionStateManager stateManager;
    private final SessionMapper sessionMapper;
    private final SessionResolveService sessionResolveService;
    private final SessionSearchResolver sessionSearchResolver;

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
     * Populate bestLapTimeMs and totalTimeMs on DTO from session summary and timestamps.
     * bestLapTimeMs is taken from SessionSummary for the player car when available.
     * totalTimeMs is computed as endedAt - startedAt for finished sessions.
     */
    private void applyBestLapAndTotalTime(Session session, SessionDto dto) {
        if (dto == null || session.getSessionUid() == null) {
            return;
        }

        // Best lap: from SessionSummary for player car (or inferred car index)
        Short carIndex = session.getPlayerCarIndex();
        if (carIndex == null) {
            Integer inferred = inferPlayerCarIndexFromData(session.getSessionUid());
            if (inferred != null) {
                carIndex = inferred.shortValue();
            }
        }
        if (carIndex != null) {
            sessionSummaryRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex)
                    .ifPresent(summary -> dto.setBestLapTimeMs(summary.getBestLapTimeMs()));
            // Fallback: when summary has no best lap (e.g. only invalid laps aggregated), derive from laps table
            if (dto.getBestLapTimeMs() == null) {
                lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                        .stream()
                        .filter(lap -> lap.getLapTimeMs() != null && Boolean.FALSE.equals(lap.getIsInvalid()))
                        .map(Lap::getLapTimeMs)
                        .mapToInt(Integer::intValue)
                        .min()
                        .ifPresent(dto::setBestLapTimeMs);
            }
        }

        // Total time: simple duration between startedAt and endedAt for finished sessions
        if (session.getStartedAt() != null && session.getEndedAt() != null) {
            long millis = Duration.between(session.getStartedAt(), session.getEndedAt()).toMillis();
            if (millis > 0) {
                dto.setTotalTimeMs(millis);
            }
        }
    }

    /**
     * List sessions with pagination (most recent first). Delegates to {@link #listSessions(SessionListFilter)}.
     */
    public List<SessionDto> listSessions(int offset, int limit) {
        return listSessions(SessionListFilter.builder().offset(offset).limit(limit).build()).getList();
    }

    /**
     * List sessions with filters, sort, and pagination. Returns list and total count for X-Total-Count header.
     * Plan: block-b-session-list-filters.md Step 7.
     */
    public SessionListResult listSessions(SessionListFilter filter) {
        log.debug("listSessions: filter sessionType={}, trackId={}, search={}, sort={}, state={}, dateFrom={}, dateTo={}, offset={}, limit={}",
                filter.getSessionType(), filter.getTrackId(), filter.getSearch(), filter.getSort(), filter.getState(),
                filter.getDateFrom(), filter.getDateTo(), filter.getOffset(), filter.getLimit());

        Short explicitSessionType = filter.getSessionType() != null ? filter.getSessionType().shortValue() : null;
        Integer explicitTrackId = filter.getTrackId();

        String searchTrimmed = filter.getSearch() != null && !filter.getSearch().isBlank() ? filter.getSearch().trim() : null;
        List<Short> searchTypeCodes = searchTrimmed != null ? sessionSearchResolver.resolveSessionTypeCodes(searchTrimmed) : List.of();
        List<Integer> searchTrackIds = searchTrimmed != null ? sessionSearchResolver.resolveTrackIds(searchTrimmed) : List.of();

        SessionListSpecification.Resolved resolved = new SessionListSpecification.Resolved(
                explicitSessionType,
                explicitTrackId,
                searchTrimmed,
                searchTypeCodes,
                searchTrackIds,
                filter.getState(),
                filter.getDateFrom(),
                filter.getDateTo(),
                filter.getSort() != null && !filter.getSort().isBlank() ? filter.getSort() : "startedAt_desc"
        );

        Specification<Session> spec = SessionListSpecification.withFilters(resolved);
        int size = Math.max(1, Math.min(filter.getLimit(), 100));
        int page = filter.getOffset() / size;
        Pageable pageable = PageRequest.of(page, size);

        long total = sessionRepository.count(spec);
        List<SessionDto> list = sessionRepository.findAll(spec, pageable).stream()
                .map(s -> {
                    SessionDto dto = sessionMapper.toDto(s, stateManager.get(s.getSessionUid()));
                    applyInferredPlayerCarIndex(s, dto);
                    applyFinishingPosition(s, dto);
                    applyBestLapAndTotalTime(s, dto);
                    return dto;
                })
                .collect(Collectors.toList());

        log.debug("listSessions: returning {} sessions, total={}", list.size(), total);
        return new SessionListResult(list, total);
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
        applyBestLapAndTotalTime(session, dto);
        dto.setParticipants(loadParticipants(session.getSessionUid()));
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
                    applyBestLapAndTotalTime(s, dto);
                    return dto;
                });
        log.debug("getActiveSession: {}", result.isPresent() ? "present" : "empty");
        return result;
    }

    /**
     * Load participants (car indices with at least one lap or summary) for Driver Comparison.
     * displayLabel from session_finishing_positions (e.g. "P1") or fallback "Car {carIndex}".
     * Block G — Driver Comparison.
     */
    public List<SessionParticipantDto> loadParticipants(Long sessionUid) {
        if (sessionUid == null) {
            return List.of();
        }
        Set<Short> carIndices = new TreeSet<>();
        lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(sessionUid).stream()
                .map(Lap::getCarIndex)
                .filter(c -> c != null)
                .forEach(carIndices::add);
        sessionSummaryRepository.findBySessionUid(sessionUid).stream()
                .map(SessionSummary::getCarIndex)
                .filter(c -> c != null)
                .forEach(carIndices::add);

        List<SessionFinishingPosition> positions =
                finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(sessionUid);
        java.util.Map<Short, String> labelByCar = positions.stream()
                .filter(p -> p.getCarIndex() != null && p.getFinishingPosition() != null)
                .collect(Collectors.toMap(SessionFinishingPosition::getCarIndex,
                        p -> "P" + p.getFinishingPosition(), (a, b) -> a));

        List<SessionParticipantDto> result = new ArrayList<>();
        for (Short carIndex : carIndices) {
            String displayLabel = labelByCar.getOrDefault(carIndex, "Car " + carIndex);
            result.add(SessionParticipantDto.builder()
                    .carIndex(carIndex.intValue())
                    .displayLabel(displayLabel)
                    .build());
        }
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
