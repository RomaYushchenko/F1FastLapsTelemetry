package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.LapMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read operations for laps: laps list, pace, tyre wear, lap trace.
 * Resolves session via SessionResolveService, uses LapRepository, CarTelemetryRawRepository,
 * TyreWearPerLapRepository, LapMapper. Filtering (e.g. lapTimeMs &gt; 0) in service/mapper.
 * See: implementation_phases.md Phase 2.2.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LapQueryService {

    private final SessionResolveService sessionResolveService;
    private final LapRepository lapRepository;
    private final CarTelemetryRawRepository carTelemetryRawRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final LapMapper lapMapper;

    public List<LapResponseDto> getLaps(String sessionId, Short carIndex) {
        log.debug("getLaps: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<LapResponseDto> result = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toLapResponseDto)
                .collect(Collectors.toList());
        log.debug("getLaps: returning {} laps", result.size());
        return result;
    }

    public List<PacePointDto> getPace(String sessionId, Short carIndex) {
        log.debug("getPace: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<PacePointDto> result = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toPacePointDto)
                .filter(p -> p != null)
                .collect(Collectors.toList());
        log.debug("getPace: returning {} pace points", result.size());
        return result;
    }

    public List<TyreWearPointDto> getTyreWear(String sessionId, Short carIndex) {
        log.debug("getTyreWear: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<TyreWearPointDto> result = tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toTyreWearPointDto)
                .collect(Collectors.toList());
        log.debug("getTyreWear: returning {} tyre wear points", result.size());
        return result;
    }

    public List<TracePointDto> getLapTrace(String sessionId, int lapNum, Short carIndex) {
        log.debug("getLapTrace: sessionId={}, lapNum={}, carIndex={}", sessionId, lapNum, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<TracePointDto> result = carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        session.getSessionUid(), carIndex, (short) lapNum)
                .stream()
                .map(lapMapper::toTracePointDto)
                .collect(Collectors.toList());
        log.debug("getLapTrace: returning {} trace points", result.size());
        return result;
    }

    private static String normalizeId(String id) {
        return id != null ? id.trim() : "";
    }
}
