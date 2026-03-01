package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.StintDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.StintMapper;
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
 * Query service for stints. Groups consecutive laps with the same compound into stints.
 * Returns list ordered by stint index.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StintQueryService {

    private final SessionResolveService sessionResolveService;
    private final LapRepository lapRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final StintMapper stintMapper;

    /**
     * Get stints for session and car. 404 if session not found.
     * Empty list if no laps or no tyre wear data.
     */
    public List<StintDto> getStints(String sessionId, Short carIndex) {
        log.debug("getStints: sessionId={}, carIndex={}", sessionId, carIndex);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(normalizeId(sessionId));
        Long sessionUid = session.getSessionUid();

        List<Lap> laps = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex);
        List<TyreWearPerLap> tyreWearList = tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(sessionUid, carIndex);

        if (laps.isEmpty() || tyreWearList.isEmpty()) {
            log.debug("getStints: no laps or tyre wear, returning empty");
            return List.of();
        }

        Map<Integer, Lap> lapByNumber = laps.stream()
                .collect(Collectors.toMap(l -> l.getLapNumber().intValue(), l -> l, (a, b) -> a));
        Map<Integer, Integer> compoundByLap = tyreWearList.stream()
                .filter(t -> t.getCompound() != null)
                .collect(Collectors.toMap(t -> t.getLapNumber().intValue(), t -> t.getCompound().intValue(), (a, b) -> a));

        List<StintDto> result = new ArrayList<>();
        int stintIndex = 1;
        int startLap = 0;
        Integer currentCompound = null;
        List<Lap> stintLaps = new ArrayList<>();

        for (Lap lap : laps) {
            int lapNum = lap.getLapNumber().intValue();
            Integer compound = compoundByLap.get(lapNum);
            if (compound == null) {
                continue;
            }
            if (currentCompound == null || !currentCompound.equals(compound)) {
                if (currentCompound != null && !stintLaps.isEmpty()) {
                    result.add(stintMapper.toDto(stintIndex++, currentCompound, startLap, stintLaps.size(), stintLaps));
                }
                currentCompound = compound;
                startLap = lapNum;
                stintLaps = new ArrayList<>();
            }
            stintLaps.add(lap);
        }
        if (currentCompound != null && !stintLaps.isEmpty()) {
            result.add(stintMapper.toDto(stintIndex, currentCompound, startLap, stintLaps.size(), stintLaps));
        }

        log.debug("getStints: returning {} stints", result.size());
        return result;
    }

    private static String normalizeId(String id) {
        return id != null ? id.trim() : "";
    }
}
