package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionEventMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Query service for session events (FTLP, PENA, SCAR, etc.).
 * Block E — Session events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionEventsQueryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final SessionEventRepository sessionEventRepository;
    private final SessionResolveService sessionResolveService;
    private final SessionEventMapper sessionEventMapper;

    /**
     * Get events for a session, optionally filtered by lap range, with limit.
     *
     * @param sessionUidOrPublicId session UID or public UUID
     * @param fromLap               optional minimum lap (inclusive)
     * @param toLap                 optional maximum lap (inclusive)
     * @param limit                 optional max number of events (default 100, max 500)
     * @return list of events ordered by lap, then frame_id
     * @throws SessionNotFoundException if session not found
     */
    public List<SessionEventDto> getEvents(String sessionUidOrPublicId, Short fromLap, Short toLap, Integer limit) {
        log.debug("getEvents: sessionUidOrPublicId={}, fromLap={}, toLap={}, limit={}",
                sessionUidOrPublicId, fromLap, toLap, limit);
        long sessionUid = sessionResolveService.getSessionByPublicIdOrUid(sessionUidOrPublicId).getSessionUid();
        int effectiveLimit = limit != null ? Math.min(Math.max(1, limit), MAX_LIMIT) : DEFAULT_LIMIT;

        List<SessionEvent> entities;
        if (fromLap != null && toLap != null) {
            entities = sessionEventRepository.findBySessionUidAndLapBetweenOrderByLapAscFrameIdAsc(sessionUid, fromLap, toLap);
        } else {
            entities = sessionEventRepository.findBySessionUidOrderByLapAscFrameIdAsc(sessionUid);
        }
        List<SessionEventDto> list = entities.stream()
                .limit(effectiveLimit)
                .map(sessionEventMapper::toDto)
                .collect(Collectors.toList());
        log.debug("getEvents: returning {} events", list.size());
        return list;
    }
}
