package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErsPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapCornerDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner.CornerSegment;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.corner.SteerBasedCornerSegmenter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.LapMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarStatusRawRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read operations for laps: laps list, pace, tyre wear, lap trace.
 * Resolves session via SessionResolveService, uses LapRepository, CarTelemetryRawRepository,
 * TyreWearPerLapRepository, LapMapper. Filtering (e.g. lapTimeMs &gt; 0) in service/mapper.
 * See: implementation_phases.md Phase 2.2; 04-session-summary-page.md Etap 5 (ERS).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LapQueryService {

    /** ERS max energy in Joules (F1 2022+ regulations). Same as CarStatusProcessor. */
    private static final float ERS_MAX_ENERGY_J = 4_000_000f;

    private final SessionResolveService sessionResolveService;
    private final LapRepository lapRepository;
    private final CarTelemetryRawRepository carTelemetryRawRepository;
    private final CarStatusRawRepository carStatusRawRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final LapMapper lapMapper;
    private final SteerBasedCornerSegmenter cornerSegmenter;

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

    /**
     * Speed vs lap distance for one lap (GET .../laps/{lapNum}/speed-trace).
     * Plan: 13-session-summary-speed-corner-graph.md Phase 1.
     */
    public List<SpeedTracePointDto> getSpeedTrace(String sessionId, int lapNum, Short carIndex) {
        log.debug("getSpeedTrace: sessionId={}, lapNum={}, carIndex={}", sessionId, lapNum, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<SpeedTracePointDto> result = carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        session.getSessionUid(), carIndex, (short) lapNum)
                .stream()
                .map(lapMapper::toSpeedTracePointDto)
                .filter(p -> p != null)
                .collect(Collectors.toList());
        log.debug("getSpeedTrace: returning {} speed trace points", result.size());
        return result;
    }

    /**
     * Per-corner metrics (entry/apex/exit speed) for one lap. Steer-based corner detection.
     * Plan: 13-session-summary-speed-corner-graph.md Phase 2.
     */
    public List<LapCornerDto> getCorners(String sessionId, int lapNum, Short carIndex) {
        log.debug("getCorners: sessionId={}, lapNum={}, carIndex={}", sessionId, lapNum, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<CarTelemetryRaw> rawList = carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        session.getSessionUid(), carIndex, (short) lapNum);

        List<SteerBasedCornerSegmenter.Point> points = rawList.stream()
                .filter(r -> r.getLapDistanceM() != null && r.getSpeedKph() != null && r.getSteer() != null)
                .map(r -> new SteerBasedCornerSegmenter.Point(
                        r.getLapDistanceM(), r.getSpeedKph().intValue(), r.getSteer()))
                .collect(Collectors.toList());
        if (points.isEmpty()) {
            log.debug("getCorners: no valid points, returning empty");
            return List.of();
        }

        List<CornerSegment> segments = cornerSegmenter.detect(points);
        List<LapCornerDto> result = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            CornerSegment seg = segments.get(i);
            int entryKph = speedAtDistance(rawList, seg.getStartDistanceM());
            int exitKph = speedAtDistance(rawList, seg.getEndDistanceM());
            int apexKph = speedAtDistance(rawList, seg.getApexDistanceM());
            result.add(LapCornerDto.builder()
                    .cornerIndex(i + 1)
                    .startDistanceM(seg.getStartDistanceM())
                    .endDistanceM(seg.getEndDistanceM())
                    .apexDistanceM(seg.getApexDistanceM())
                    .entrySpeedKph(entryKph)
                    .apexSpeedKph(apexKph)
                    .exitSpeedKph(exitKph)
                    .build());
        }
        log.debug("getCorners: returning {} corners", result.size());
        return result;
    }

    /** Nearest speed (kph) at given distance from raw list (ordered by lap_distance_m). */
    private static int speedAtDistance(List<CarTelemetryRaw> rawList, float distanceM) {
        if (rawList.isEmpty()) return 0;
        CarTelemetryRaw best = rawList.get(0);
        float bestDist = best.getLapDistanceM() != null ? Math.abs(best.getLapDistanceM() - distanceM) : Float.MAX_VALUE;
        for (int i = 1; i < rawList.size(); i++) {
            CarTelemetryRaw r = rawList.get(i);
            if (r.getLapDistanceM() == null || r.getSpeedKph() == null) continue;
            float d = Math.abs(r.getLapDistanceM() - distanceM);
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best.getSpeedKph() != null ? best.getSpeedKph().intValue() : 0;
    }

    /**
     * ERS energy store % along the lap (merge telemetry lap_distance with car_status ers_store_energy by timestamp).
     * Plan: 04-session-summary-page.md Etap 5.
     */
    public List<ErsPointDto> getLapErs(String sessionId, int lapNum, Short carIndex) {
        log.debug("getLapErs: sessionId={}, lapNum={}, carIndex={}", sessionId, lapNum, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        Long sessionUid = session.getSessionUid();
        short lapNumShort = (short) lapNum;

        List<CarTelemetryRaw> telemetryList = carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        sessionUid, carIndex, lapNumShort);
        if (telemetryList.isEmpty()) {
            log.debug("getLapErs: no telemetry for lap, returning empty");
            return List.of();
        }

        Instant tsMin = telemetryList.get(0).getTs();
        Instant tsMax = telemetryList.get(telemetryList.size() - 1).getTs();
        List<CarStatusRaw> statusList = carStatusRawRepository
                .findBySessionUidAndCarIndexAndTsBetweenOrderByTsAsc(sessionUid, carIndex, tsMin, tsMax);
        if (statusList.isEmpty()) {
            log.debug("getLapErs: no car status in lap time range, returning empty");
            return List.of();
        }

        List<ErsPointDto> result = new ArrayList<>();
        for (CarTelemetryRaw tel : telemetryList) {
            CarStatusRaw nearest = findNearestByTs(statusList, tel.getTs());
            if (nearest == null || nearest.getErsStoreEnergy() == null) {
                continue;
            }
            float percent = 100f * nearest.getErsStoreEnergy() / ERS_MAX_ENERGY_J;
            int energyPercent = (int) Math.max(0, Math.min(100, Math.round(percent)));
            Float lapDistanceM = tel.getLapDistanceM();
            if (lapDistanceM != null) {
                result.add(ErsPointDto.builder()
                        .lapDistanceM(lapDistanceM)
                        .energyPercent(energyPercent)
                        .build());
            }
        }
        log.debug("getLapErs: returning {} ERS points", result.size());
        return result;
    }

    private static CarStatusRaw findNearestByTs(List<CarStatusRaw> sortedByTs, Instant ts) {
        if (sortedByTs.isEmpty()) {
            return null;
        }
        long tsEpoch = ts.toEpochMilli();
        CarStatusRaw best = sortedByTs.get(0);
        long bestDiff = Math.abs(best.getTs().toEpochMilli() - tsEpoch);
        for (int i = 1; i < sortedByTs.size(); i++) {
            CarStatusRaw s = sortedByTs.get(i);
            long diff = Math.abs(s.getTs().toEpochMilli() - tsEpoch);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = s;
            }
        }
        return best;
    }

    private static String normalizeId(String id) {
        return id != null ? id.trim() : "";
    }
}
