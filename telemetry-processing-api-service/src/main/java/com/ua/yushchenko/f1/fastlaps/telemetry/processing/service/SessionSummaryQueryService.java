package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionSummaryMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Read operation for session summary by session id + carIndex.
 * Resolves session via SessionResolveService; returns empty summary when session exists but no laps aggregated yet.
 * See: implementation_phases.md Phase 2.3.
 */
@Service
@RequiredArgsConstructor
public class SessionSummaryQueryService {

    private final SessionResolveService sessionResolveService;
    private final SessionSummaryRepository summaryRepository;
    private final SessionSummaryMapper sessionSummaryMapper;

    public SessionSummaryDto getSummary(String sessionId, Short carIndex) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(sessionId != null ? sessionId.trim() : "");
        return summaryRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex)
                .map(sessionSummaryMapper::toDto)
                .orElse(SessionSummaryMapper.emptySummaryDto());
    }
}
