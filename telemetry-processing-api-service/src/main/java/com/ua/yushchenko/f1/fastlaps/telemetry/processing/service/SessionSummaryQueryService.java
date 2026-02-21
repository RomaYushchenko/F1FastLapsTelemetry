package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionSummaryMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private final SessionFinishingPositionRepository finishingPositionRepository;
    private final SessionSummaryMapper sessionSummaryMapper;

    public SessionSummaryDto getSummary(String sessionId, Short carIndex) {
        log.debug("getSummary: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(sessionId != null ? sessionId.trim() : "");
        Optional<SessionSummary> opt =
                summaryRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex);
        SessionSummaryDto result = opt.map(sessionSummaryMapper::toDto)
                .orElse(SessionSummaryMapper.emptySummaryDto());
        enrichWithLeader(session, result);
        log.debug("getSummary: {}", opt.isPresent() ? "found summary" : "empty summary");
        return result;
    }

    /**
     * Enrich summary with leader (P1) from session_finishing_positions when session is finished.
     */
    private void enrichWithLeader(Session session, SessionSummaryDto result) {
        List<SessionFinishingPosition> positions =
                finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(session.getSessionUid());
        // We only persist the player car's finishing position; when we persist all cars, there will be a row with position=1.
        SessionFinishingPosition leader = positions.stream()
                .filter(p -> p.getFinishingPosition() != null && p.getFinishingPosition() == 1)
                .findFirst()
                .orElse(null);
        if (leader == null) {
            return;
        }
        result.setLeaderPosition(1);
        result.setLeaderCarIndex(leader.getCarIndex() != null ? leader.getCarIndex().intValue() : null);
        Short playerCarIndex = session.getPlayerCarIndex();
        result.setLeaderIsPlayer(
                playerCarIndex != null && leader.getCarIndex() != null
                        && playerCarIndex.equals(leader.getCarIndex()));
        result.setLeaderDriverName(null);
        result.setLeaderTeamName(null);
    }
}
