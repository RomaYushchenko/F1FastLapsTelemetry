package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.RaceOverviewChartRowDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.RaceOverviewDriverDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionRaceOverviewDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.TyreCompoundMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionFinishingPosition;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionFinishingPositionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.util.LeaderboardTimingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Post-session race overview: leaderboard from DB, position and cumulative gap-to-leader chart series.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceOverviewQueryService {

    private static final String GAP_EMPTY = "—";

    private static final String[] CHART_COLOR_PALETTE = {
            "#00E5FF", "#00FF85", "#E10600", "#FACC15", "#A855F7", "#F472B6", "#22D3EE", "#FB923C",
            "#4ADE80", "#818CF8", "#2DD4BF", "#FBBF24", "#EF4444", "#38BDF8", "#C084FC", "#34D399",
            "#F97316", "#EC4899", "#14B8A6", "#84CC16", "#64748B"
    };

    private final SessionResolveService sessionResolveService;
    private final LapRepository lapRepository;
    private final SessionFinishingPositionRepository finishingPositionRepository;
    private final SessionDriverRepository sessionDriverRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final SessionSummaryRepository sessionSummaryRepository;

    /**
     * Leaderboard for a finished or past session from DB (cumulative gap to P1 on the final classification order).
     */
    public List<LeaderboardEntryDto> getLeaderboardForSession(String sessionId) {
        log.debug("getLeaderboardForSession: id={}", sessionId);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(sessionId);
        List<LeaderboardEntryDto> entries = buildLeaderboardEntries(session);
        log.debug("getLeaderboardForSession: sessionUid={}, entries={}", session.getSessionUid(), entries.size());
        return entries;
    }

    /**
     * Full race overview including chart rows aligned with {@link SessionRaceOverviewDto#getDrivers()} order.
     */
    public SessionRaceOverviewDto getRaceOverview(String sessionId) {
        log.debug("getRaceOverview: id={}", sessionId);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(sessionId);
        Long sessionUid = session.getSessionUid();
        List<Lap> allLaps = lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(sessionUid);
        List<LeaderboardEntryDto> entries = buildLeaderboardEntries(session, allLaps);
        List<Integer> carOrder = entries.stream()
                .map(LeaderboardEntryDto::getCarIndex)
                .filter(Objects::nonNull)
                .toList();

        List<RaceOverviewDriverDto> drivers = new ArrayList<>();
        for (int i = 0; i < carOrder.size(); i++) {
            int carIndex = carOrder.get(i);
            String label = entries.stream()
                    .filter(e -> e.getCarIndex() != null && e.getCarIndex() == carIndex)
                    .map(LeaderboardEntryDto::getDriverLabel)
                    .findFirst()
                    .orElse("Car " + carIndex);
            drivers.add(RaceOverviewDriverDto.builder()
                    .carIndex(carIndex)
                    .displayLabel(label)
                    .colorHex(CHART_COLOR_PALETTE[i % CHART_COLOR_PALETTE.length])
                    .build());
        }

        int maxLap = allLaps.stream()
                .map(Lap::getLapNumber)
                .filter(Objects::nonNull)
                .mapToInt(Short::intValue)
                .max()
                .orElse(0);

        Map<Integer, TreeMap<Integer, Integer>> positionAtLapStart = positionAtLapStartByCar(allLaps);
        Map<Integer, TreeMap<Integer, Integer>> cumulativeMsByLapEnd = cumulativeRaceMsByLapEnd(allLaps);

        List<RaceOverviewChartRowDto> positionRows = new ArrayList<>();
        List<RaceOverviewChartRowDto> gapRows = new ArrayList<>();
        Map<Integer, Integer> lastPositionForward = new HashMap<>();

        for (int lap = 1; lap <= maxLap; lap++) {
            List<Double> posVals = new ArrayList<>();
            for (int carIndex : carOrder) {
                TreeMap<Integer, Integer> perCar = positionAtLapStart.get(carIndex);
                Integer atLap = perCar != null ? perCar.get(lap) : null;
                if (atLap != null && atLap > 0) {
                    lastPositionForward.put(carIndex, atLap);
                }
                Integer filled = atLap != null && atLap > 0 ? atLap : lastPositionForward.get(carIndex);
                posVals.add(filled != null ? filled.doubleValue() : null);
            }
            positionRows.add(RaceOverviewChartRowDto.builder().lapNumber(lap).values(posVals).build());

            List<Double> gapVals = new ArrayList<>();
            Integer leaderCum = null;
            for (int carIndex : carOrder) {
                TreeMap<Integer, Integer> cumMap = cumulativeMsByLapEnd.get(carIndex);
                Integer cum = cumMap != null ? cumMap.get(lap) : null;
                if (cum != null && cum > 0) {
                    if (leaderCum == null || cum < leaderCum) {
                        leaderCum = cum;
                    }
                }
            }
            for (int carIndex : carOrder) {
                TreeMap<Integer, Integer> cumMap = cumulativeMsByLapEnd.get(carIndex);
                Integer cum = cumMap != null ? cumMap.get(lap) : null;
                if (cum == null || cum <= 0 || leaderCum == null) {
                    gapVals.add(null);
                } else {
                    gapVals.add((cum - leaderCum) / 1000.0);
                }
            }
            gapRows.add(RaceOverviewChartRowDto.builder().lapNumber(lap).values(gapVals).build());
        }

        SessionRaceOverviewDto dto = SessionRaceOverviewDto.builder()
                .sessionUid(SessionMapper.toPublicIdString(session))
                .entries(entries)
                .drivers(drivers)
                .positionChartRows(positionRows)
                .gapChartRows(gapRows)
                .build();
        log.debug("getRaceOverview: sessionUid={}, drivers={}, maxLap={}", sessionUid, drivers.size(), maxLap);
        return dto;
    }

    private List<LeaderboardEntryDto> buildLeaderboardEntries(Session session) {
        List<Lap> allLaps = lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(session.getSessionUid());
        return buildLeaderboardEntries(session, allLaps);
    }

    private List<LeaderboardEntryDto> buildLeaderboardEntries(Session session, List<Lap> allLaps) {
        Long sessionUid = session.getSessionUid();
        if (allLaps.isEmpty()) {
            return List.of();
        }

        Map<Integer, Lap> lastLapByCar = lastCompletedLapPerCar(allLaps);
        Map<Integer, String> labelByCarIndex = new HashMap<>();
        for (SessionDriver sd : sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(sessionUid)) {
            if (sd.getCarIndex() != null && sd.getDriverLabel() != null && !sd.getDriverLabel().isBlank()) {
                labelByCarIndex.put(sd.getCarIndex().intValue(), sd.getDriverLabel().trim());
            }
        }

        List<SessionFinishingPosition> finishing = finishingPositionRepository.findBySessionUidOrderByFinishingPositionAsc(sessionUid);
        Map<Integer, SessionFinishingPosition> fpByCar = new HashMap<>();
        for (SessionFinishingPosition fp : finishing) {
            if (fp.getCarIndex() != null) {
                fpByCar.put(fp.getCarIndex().intValue(), fp);
            }
        }

        Map<Integer, Short> lastCompoundFromTyreWear = lastActualCompoundByCarFromTyreWear(sessionUid);

        Set<Integer> lapCars = allLaps.stream()
                .map(Lap::getCarIndex)
                .filter(Objects::nonNull)
                .map(Short::intValue)
                .collect(Collectors.toSet());

        List<Integer> carOrder = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (SessionFinishingPosition fp : finishing) {
            if (fp.getCarIndex() == null) {
                continue;
            }
            int c = fp.getCarIndex().intValue();
            if (lapCars.contains(c) && seen.add(c)) {
                carOrder.add(c);
            }
        }
        List<Integer> remaining = lapCars.stream()
                .filter(c -> !seen.contains(c))
                .sorted(Comparator.comparingInt(c -> LeaderboardTimingHelper.totalRaceTimeMs(c, allLaps)))
                .toList();
        carOrder.addAll(remaining);

        Map<Integer, Integer> totalMsByCar = new HashMap<>();
        for (int c : carOrder) {
            totalMsByCar.put(c, LeaderboardTimingHelper.totalRaceTimeMs(c, allLaps));
        }

        Integer leaderTotalMs = null;
        Integer leaderLastLapMs = null;
        if (!carOrder.isEmpty()) {
            int leaderCar = carOrder.get(0);
            int lt = totalMsByCar.getOrDefault(leaderCar, 0);
            if (lt > 0) {
                leaderTotalMs = lt;
            }
            Lap leaderLastLap = lastLapByCar.get(leaderCar);
            if (leaderLastLap != null && leaderLastLap.getLapTimeMs() != null && leaderLastLap.getLapTimeMs() > 0) {
                leaderLastLapMs = leaderLastLap.getLapTimeMs();
            }
        }

        Map<Integer, Integer> bestFromSummaryByCar = new HashMap<>();
        for (SessionSummary ss : sessionSummaryRepository.findBySessionUid(sessionUid)) {
            if (ss.getCarIndex() != null && ss.getBestLapTimeMs() != null && ss.getBestLapTimeMs() > 0) {
                bestFromSummaryByCar.put(ss.getCarIndex().intValue(), ss.getBestLapTimeMs());
            }
        }

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int position = 1;
        for (int carIndex : carOrder) {
            Lap lastLap = lastLapByCar.get(carIndex);
            SessionFinishingPosition fp = fpByCar.get(carIndex);
            String compound = compoundFromFinishing(fp);
            if (GAP_EMPTY.equals(compound)) {
                Short ac = lastCompoundFromTyreWear.get(carIndex);
                String fromWear = TyreCompoundMapper.toPersistedFromActualCompound(ac);
                if (fromWear != null) {
                    compound = fromWear;
                }
            }
            String driverLabel = labelByCarIndex.get(carIndex);
            if (driverLabel == null || driverLabel.isBlank()) {
                if (fp != null && fp.getFinishingPosition() != null) {
                    driverLabel = "P" + fp.getFinishingPosition();
                } else {
                    driverLabel = "Car " + carIndex;
                }
            }
            int carTotalMs = totalMsByCar.getOrDefault(carIndex, 0);
            String gap = LeaderboardTimingHelper.formatCumulativeRaceGap(position, leaderTotalMs, carTotalMs);
            String lastLapGap = LeaderboardTimingHelper.formatLastLapGap(
                    position,
                    leaderLastLapMs,
                    lastLap != null ? lastLap.getLapTimeMs() : null);
            Integer bestLapMs = LeaderboardTimingHelper.resolveBestLapTimeMs(
                    bestFromSummaryByCar.get(carIndex),
                    LeaderboardTimingHelper.bestLapTimeMsFromLaps(carIndex, allLaps));
            Integer totalRaceMsDto = carTotalMs > 0 ? carTotalMs : null;

            entries.add(LeaderboardEntryDto.builder()
                    .position(position)
                    .carIndex(carIndex)
                    .driverLabel(driverLabel)
                    .compound(compound)
                    .gap(gap)
                    .lastLapGap(lastLapGap)
                    .bestLapTimeMs(bestLapMs)
                    .totalRaceTimeMs(totalRaceMsDto)
                    .lastLapTimeMs(lastLap != null ? lastLap.getLapTimeMs() : null)
                    .sector1Ms(lastLap != null ? lastLap.getSector1TimeMs() : null)
                    .sector2Ms(lastLap != null ? lastLap.getSector2TimeMs() : null)
                    .sector3Ms(lastLap != null ? lastLap.getSector3TimeMs() : null)
                    .build());
            position++;
        }
        return entries;
    }

    private Map<Integer, Short> lastActualCompoundByCarFromTyreWear(long sessionUid) {
        List<TyreWearPerLap> rows = tyreWearPerLapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(sessionUid);
        Map<Integer, Short> out = new HashMap<>();
        for (TyreWearPerLap row : rows) {
            if (row.getCarIndex() != null && row.getCompound() != null) {
                out.put(row.getCarIndex().intValue(), row.getCompound());
            }
        }
        return out;
    }

    private static String compoundFromFinishing(SessionFinishingPosition fp) {
        if (fp == null || fp.getTyreCompound() == null || fp.getTyreCompound().isBlank()) {
            return GAP_EMPTY;
        }
        String t = fp.getTyreCompound().trim().toUpperCase();
        if ("S".equals(t) || "M".equals(t) || "H".equals(t)) {
            return t;
        }
        return GAP_EMPTY;
    }

    private static Map<Integer, Lap> lastCompletedLapPerCar(List<Lap> allLaps) {
        Map<Integer, Lap> lastByCar = new HashMap<>();
        for (Lap lap : allLaps) {
            if (lap.getCarIndex() == null || lap.getLapTimeMs() == null || lap.getLapTimeMs() <= 0) {
                continue;
            }
            int car = lap.getCarIndex().intValue();
            Lap existing = lastByCar.get(car);
            if (existing == null || (lap.getLapNumber() != null
                    && (existing.getLapNumber() == null || lap.getLapNumber() > existing.getLapNumber()))) {
                lastByCar.put(car, lap);
            }
        }
        return lastByCar;
    }

    private static Map<Integer, TreeMap<Integer, Integer>> positionAtLapStartByCar(List<Lap> allLaps) {
        Map<Integer, TreeMap<Integer, Integer>> out = new HashMap<>();
        for (Lap lap : allLaps) {
            if (lap.getCarIndex() == null || lap.getLapNumber() == null) {
                continue;
            }
            if (lap.getPositionAtLapStart() == null || lap.getPositionAtLapStart() <= 0) {
                continue;
            }
            int car = lap.getCarIndex().intValue();
            int ln = lap.getLapNumber().intValue();
            out.computeIfAbsent(car, k -> new TreeMap<>()).put(ln, lap.getPositionAtLapStart());
        }
        return out;
    }

    /**
     * After completing lap {@code n}, cumulative race time in ms (sum of valid lap times for laps 1..n).
     */
    private static Map<Integer, TreeMap<Integer, Integer>> cumulativeRaceMsByLapEnd(List<Lap> allLaps) {
        Map<Integer, List<Lap>> byCar = allLaps.stream()
                .filter(l -> l.getCarIndex() != null && l.getLapNumber() != null)
                .collect(Collectors.groupingBy(l -> l.getCarIndex().intValue()));
        Map<Integer, TreeMap<Integer, Integer>> out = new HashMap<>();
        for (Map.Entry<Integer, List<Lap>> e : byCar.entrySet()) {
            List<Lap> laps = e.getValue().stream()
                    .sorted(Comparator.comparingInt(l -> l.getLapNumber().intValue()))
                    .toList();
            TreeMap<Integer, Integer> cum = new TreeMap<>();
            int sum = 0;
            for (Lap lap : laps) {
                if (Boolean.TRUE.equals(lap.getIsInvalid())) {
                    continue;
                }
                if (lap.getLapTimeMs() == null || lap.getLapTimeMs() <= 0) {
                    continue;
                }
                sum += lap.getLapTimeMs();
                cum.put(lap.getLapNumber().intValue(), sum);
            }
            if (!cum.isEmpty()) {
                out.put(e.getKey(), cum);
            }
        }
        return out;
    }
}
