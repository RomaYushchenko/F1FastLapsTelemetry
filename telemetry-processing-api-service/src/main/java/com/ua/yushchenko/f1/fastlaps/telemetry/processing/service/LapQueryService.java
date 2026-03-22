package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErsByLapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErsPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.FuelByLapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.LapMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarStatusRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
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
     */
    public List<SpeedTracePointDto> getSpeedTrace(String sessionId, int lapNum, Short carIndex) {
        log.debug("getSpeedTrace: sessionId={}, lapNum={}, carIndex={}", sessionId, lapNum, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        List<CarTelemetryRaw> speedRows = carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        session.getSessionUid(), carIndex, (short) lapNum);
        List<SpeedTracePointDto> result = new ArrayList<>(speedRows.size());
        int seq = 0;
        for (CarTelemetryRaw row : speedRows) {
            SpeedTracePointDto p = lapMapper.toSpeedTracePointDto(row, seq++);
            if (p != null) {
                result.add(p);
            }
        }
        log.debug("getSpeedTrace: returning {} speed trace points", result.size());
        return result;
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

    /**
     * Fuel remaining at lap end for each lap (B6). Uses CarStatusRaw.fuelInTank at nearest timestamp to lap endedAt.
     */
    public List<FuelByLapDto> getFuelByLap(String sessionId, Short carIndex) {
        log.debug("getFuelByLap: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        Long sessionUid = session.getSessionUid();
        List<Lap> laps = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex);
        List<Lap> lapsWithEnd = laps.stream().filter(l -> l.getEndedAt() != null).collect(Collectors.toList());
        if (lapsWithEnd.isEmpty()) {
            log.debug("getFuelByLap: no laps with endedAt, returning empty");
            return List.of();
        }
        Instant tsMin = lapsWithEnd.get(0).getEndedAt();
        Instant tsMax = lapsWithEnd.get(lapsWithEnd.size() - 1).getEndedAt();
        List<CarStatusRaw> statusList = carStatusRawRepository
                .findBySessionUidAndCarIndexAndTsBetweenOrderByTsAsc(sessionUid, carIndex, tsMin, tsMax);
        if (statusList.isEmpty()) {
            log.debug("getFuelByLap: no car status in lap end range, returning empty");
            return List.of();
        }
        List<FuelByLapDto> result = new ArrayList<>();
        for (Lap lap : lapsWithEnd) {
            CarStatusRaw nearest = findNearestByTs(statusList, lap.getEndedAt());
            if (nearest != null && nearest.getFuelInTank() != null) {
                result.add(FuelByLapDto.builder()
                        .lapNumber(lap.getLapNumber() != null ? lap.getLapNumber().intValue() : null)
                        .fuelKg(nearest.getFuelInTank())
                        .build());
            }
        }
        log.debug("getFuelByLap: returning {} points", result.size());
        return result;
    }

    /**
     * ERS store % at lap end for each lap (B7). One value per lap; uses CarStatusRaw.ersStoreEnergy at nearest timestamp to lap endedAt.
     */
    public List<ErsByLapDto> getErsByLap(String sessionId, Short carIndex) {
        log.debug("getErsByLap: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        Long sessionUid = session.getSessionUid();
        List<Lap> laps = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex);
        List<Lap> lapsWithEnd = laps.stream().filter(l -> l.getEndedAt() != null).collect(Collectors.toList());
        if (lapsWithEnd.isEmpty()) {
            log.debug("getErsByLap: no laps with endedAt, returning empty");
            return List.of();
        }
        Instant tsMin = lapsWithEnd.get(0).getEndedAt();
        Instant tsMax = lapsWithEnd.get(lapsWithEnd.size() - 1).getEndedAt();
        List<CarStatusRaw> statusList = carStatusRawRepository
                .findBySessionUidAndCarIndexAndTsBetweenOrderByTsAsc(sessionUid, carIndex, tsMin, tsMax);
        if (statusList.isEmpty()) {
            log.debug("getErsByLap: no car status in lap end range, returning empty");
            return List.of();
        }
        List<ErsByLapDto> result = new ArrayList<>();
        for (Lap lap : lapsWithEnd) {
            CarStatusRaw nearest = findNearestByTs(statusList, lap.getEndedAt());
            if (nearest != null && nearest.getErsStoreEnergy() != null) {
                float percent = 100f * nearest.getErsStoreEnergy() / ERS_MAX_ENERGY_J;
                int energyPercent = (int) Math.max(0, Math.min(100, Math.round(percent)));
                result.add(ErsByLapDto.builder()
                        .lapNumber(lap.getLapNumber() != null ? lap.getLapNumber().intValue() : null)
                        .ersStorePercentEnd(energyPercent)
                        .build());
            }
        }
        log.debug("getErsByLap: returning {} points", result.size());
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
