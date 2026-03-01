package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PitStopDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.PitStopMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query service for pit stops. Detects pits from compound change between consecutive laps;
 * in/out lap times from Lap table. Returns list ordered by lap number.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PitStopQueryService {

    private final SessionResolveService sessionResolveService;
    private final LapRepository lapRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final PitStopMapper pitStopMapper;

    /**
     * Get pit stops for session and car. 404 if session not found (via SessionResolveService).
     * Empty list if no compound changes (no pit stops).
     */
    public List<PitStopDto> getPitStops(String sessionId, Short carIndex) {
        log.debug("getPitStops: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        Long sessionUid = session.getSessionUid();

        List<Lap> laps = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex);
        List<TyreWearPerLap> tyreWearList = tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex);

        Map<Integer, Lap> lapByNumber = laps.stream()
                .collect(Collectors.toMap(l -> l.getLapNumber().intValue(), l -> l, (a, b) -> a));
        Map<Integer, Integer> compoundByLap = tyreWearList.stream()
                .filter(t -> t.getCompound() != null)
                .collect(Collectors.toMap(t -> t.getLapNumber().intValue(), t -> t.getCompound().intValue(), (a, b) -> a));

        List<PitStopDto> result = new ArrayList<>();
        for (Lap lap : laps) {
            int lapNum = lap.getLapNumber().intValue();
            Integer compoundOut = compoundByLap.get(lapNum);
            Integer compoundIn = lapNum > 1 ? compoundByLap.get(lapNum - 1) : null;
            if (compoundIn == null || compoundOut == null || compoundIn.equals(compoundOut)) {
                continue;
            }
            Lap inLap = lapByNumber.get(lapNum - 1);
            PitStopDto dto = pitStopMapper.toDto(inLap, lap, compoundIn, compoundOut);
            if (dto != null) {
                result.add(dto);
            }
        }
        log.debug("getPitStops: returning {} pit stops", result.size());
        return result;
    }

    private static String normalizeId(String id) {
        return id != null ? id.trim() : "";
    }
}
