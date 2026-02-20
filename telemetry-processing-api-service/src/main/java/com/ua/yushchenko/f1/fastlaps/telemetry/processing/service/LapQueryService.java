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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read operations for laps: laps list, pace, tyre wear, lap trace.
 * Resolves session via SessionResolveService, uses LapRepository, CarTelemetryRawRepository,
 * TyreWearPerLapRepository, LapMapper. Filtering (e.g. lapTimeMs &gt; 0) in service/mapper.
 * See: implementation_phases.md Phase 2.2.
 */
@Service
@RequiredArgsConstructor
public class LapQueryService {

    private final SessionResolveService sessionResolveService;
    private final LapRepository lapRepository;
    private final CarTelemetryRawRepository carTelemetryRawRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final LapMapper lapMapper;

    public List<LapResponseDto> getLaps(String sessionId, Short carIndex) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        return lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toLapResponseDto)
                .collect(Collectors.toList());
    }

    public List<PacePointDto> getPace(String sessionId, Short carIndex) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        return lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toPacePointDto)
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    public List<TyreWearPointDto> getTyreWear(String sessionId, Short carIndex) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        return tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toTyreWearPointDto)
                .collect(Collectors.toList());
    }

    public List<TracePointDto> getLapTrace(String sessionId, int lapNum, Short carIndex) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        return carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        session.getSessionUid(), carIndex, (short) lapNum)
                .stream()
                .map(lapMapper::toTracePointDto)
                .collect(Collectors.toList());
    }

    private static String normalizeId(String id) {
        return id != null ? id.trim() : "";
    }
}
