package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ComparisonResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read operation for Driver Comparison: two cars' laps, summary, pace, trace and speed trace.
 * Default reference laps: best lap of each pilot; optional query params override.
 * Block G — Driver Comparison. See: block-g-driver-comparison.md Step 23.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonQueryService {

    private final SessionResolveService sessionResolveService;
    private final LapQueryService lapQueryService;
    private final SessionSummaryQueryService sessionSummaryQueryService;

    /**
     * Get comparison data for two cars. When referenceLapNumA/B are null, uses best lap from summary for each car.
     *
     * @throws IllegalArgumentException if carIndexA == carIndexB or car params missing
     * @throws SessionNotFoundException if session not found or no data for one of the cars
     */
    public ComparisonResponseDto getComparison(
            String sessionId,
            Integer carIndexA,
            Integer carIndexB,
            Integer referenceLapNumA,
            Integer referenceLapNumB
    ) {
        log.debug("getComparison: sessionUid={}, carIndexA={}, carIndexB={}, referenceLapNumA={}, referenceLapNumB={}",
                sessionId, carIndexA, carIndexB, referenceLapNumA, referenceLapNumB);

        if (carIndexA == null || carIndexB == null) {
            log.warn("Comparison requires both carIndexA and carIndexB");
            throw new IllegalArgumentException("carIndexA and carIndexB are required");
        }
        if (carIndexA.equals(carIndexB)) {
            log.warn("carIndexA must not equal carIndexB: {}", carIndexA);
            throw new IllegalArgumentException("carIndexA and carIndexB must be different");
        }

        String normalizedId = sessionId != null ? sessionId.trim() : "";
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizedId);
        String sessionUidStr = SessionMapper.toPublicIdString(session);
        Short carA = carIndexA.shortValue();
        Short carB = carIndexB.shortValue();

        SessionSummaryDto summaryA = sessionSummaryQueryService.getSummary(sessionId, carA);
        SessionSummaryDto summaryB = sessionSummaryQueryService.getSummary(sessionId, carB);

        if (summaryA.getTotalLaps() == null || summaryA.getTotalLaps() == 0) {
            if (lapQueryService.getLaps(sessionId, carA).isEmpty()) {
                log.warn("No data for car A (carIndex={}) in session {}", carIndexA, sessionUidStr);
                throw new SessionNotFoundException("No laps or summary for car index " + carIndexA);
            }
        }
        if (summaryB.getTotalLaps() == null || summaryB.getTotalLaps() == 0) {
            if (lapQueryService.getLaps(sessionId, carB).isEmpty()) {
                log.warn("No data for car B (carIndex={}) in session {}", carIndexB, sessionUidStr);
                throw new SessionNotFoundException("No laps or summary for car index " + carIndexB);
            }
        }

        List<LapResponseDto> lapsA = lapQueryService.getLaps(sessionId, carA);
        List<LapResponseDto> lapsB = lapQueryService.getLaps(sessionId, carB);
        List<PacePointDto> paceA = lapQueryService.getPace(sessionId, carA);
        List<PacePointDto> paceB = lapQueryService.getPace(sessionId, carB);

        int refLapA = referenceLapNumA != null
                ? referenceLapNumA
                : (summaryA.getBestLapNumber() != null ? summaryA.getBestLapNumber() : 1);
        int refLapB = referenceLapNumB != null
                ? referenceLapNumB
                : (summaryB.getBestLapNumber() != null ? summaryB.getBestLapNumber() : 1);

        List<TracePointDto> traceA = lapQueryService.getLapTrace(sessionId, refLapA, carA);
        List<TracePointDto> traceB = lapQueryService.getLapTrace(sessionId, refLapB, carB);
        List<SpeedTracePointDto> speedTraceA = lapQueryService.getSpeedTrace(sessionId, refLapA, carA);
        List<SpeedTracePointDto> speedTraceB = lapQueryService.getSpeedTrace(sessionId, refLapB, carB);

        ComparisonResponseDto dto = ComparisonResponseDto.builder()
                .sessionUid(sessionUidStr)
                .carIndexA(carIndexA)
                .carIndexB(carIndexB)
                .lapsA(lapsA)
                .lapsB(lapsB)
                .summaryA(summaryA)
                .summaryB(summaryB)
                .paceA(paceA)
                .paceB(paceB)
                .referenceLapNumA(refLapA)
                .referenceLapNumB(refLapB)
                .traceA(traceA)
                .traceB(traceB)
                .speedTraceA(speedTraceA)
                .speedTraceB(speedTraceB)
                .build();

        log.debug("getComparison: returning comparison for session {}", sessionUidStr);
        return dto;
    }
}
