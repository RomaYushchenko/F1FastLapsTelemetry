package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionSummaryMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Read operation for session summary by session id + carIndex.
 * Resolves session via SessionResolveService; returns empty summary when session exists but no laps aggregated yet.
 * See: implementation_phases.md Phase 2.3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryQueryService {

    private final SessionResolveService sessionResolveService;
    private final SessionSummaryRepository summaryRepository;
    private final SessionSummaryMapper sessionSummaryMapper;

    public SessionSummaryDto getSummary(String sessionId, Short carIndex) {
        log.debug("getSummary: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(sessionId != null ? sessionId.trim() : "");
        Optional<SessionSummary> opt =
                summaryRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex);
        SessionSummaryDto result = opt.map(sessionSummaryMapper::toDto)
                .orElse(SessionSummaryMapper.emptySummaryDto());
        log.debug("getSummary: {}", opt.isPresent() ? "found summary" : "empty summary");
        return result;
    }
}
