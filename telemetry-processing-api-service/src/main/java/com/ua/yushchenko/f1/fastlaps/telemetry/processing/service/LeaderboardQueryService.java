package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.CarPositionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.TyreCompoundMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.LastTyreCompoundState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.util.LeaderboardTimingHelper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds live leaderboard for the active session: positions from state, last lap from DB,
 * best lap from session summary or laps, cumulative total time vs P1 and separate last-lap gap vs P1,
 * compound from CarStatus visual snapshot with fallback to last actual compound from Car Status,
 * driver label from session_drivers (fallback "Car N").
 * Block E — Live leaderboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardQueryService {

    private static final String GAP_EMPTY = "—";

    private final SessionStateManager stateManager;
    private final LapRepository lapRepository;
    private final SessionDriverRepository sessionDriverRepository;
    private final SessionSummaryRepository sessionSummaryRepository;
    private final LastTyreCompoundState lastTyreCompoundState;

    /**
     * Get leaderboard for the current active session, or empty list if none.
     * Entries sorted by position (1-based). Gap to leader; compound S/M/H from snapshot.
     */
    public List<LeaderboardEntryDto> getLeaderboardForActiveSession() {
        log.debug("getLeaderboardForActiveSession");
        Map<Long, SessionRuntimeState> active = stateManager.getAllActive();
        Optional<Map.Entry<Long, SessionRuntimeState>> first = active.entrySet().stream()
                .filter(e -> e.getValue().isActive())
                .findFirst();
        if (first.isEmpty()) {
            log.debug("getLeaderboardForActiveSession: no active session");
            return List.of();
        }
        Long sessionUid = first.get().getKey();
        SessionRuntimeState state = first.get().getValue();
        List<LeaderboardEntryDto> list = buildLeaderboard(sessionUid, state);
        log.debug("getLeaderboardForActiveSession: sessionUid={}, entries={}", sessionUid, list.size());
        return list;
    }

    /**
     * Get live positions for the current active session (B9), or empty list if none.
     */
    public List<CarPositionDto> getPositionsForActiveSession() {
        log.debug("getPositionsForActiveSession");
        Map<Long, SessionRuntimeState> active = stateManager.getAllActive();
        Optional<Map.Entry<Long, SessionRuntimeState>> first = active.entrySet().stream()
                .filter(e -> e.getValue().isActive())
                .findFirst();
        if (first.isEmpty()) {
            log.debug("getPositionsForActiveSession: no active session");
            return List.of();
        }
        SessionRuntimeState state = first.get().getValue();
        List<CarPositionDto> positions = state.getLatestPositions();
        log.debug("getPositionsForActiveSession: positions={}", positions.size());
        return positions;
    }

    /**
     * Build leaderboard for a given session and its runtime state.
     * Used by REST and by LiveDataBroadcaster for WebSocket push.
     */
    public List<LeaderboardEntryDto> buildLeaderboard(Long sessionUid, SessionRuntimeState state) {
        log.debug("buildLeaderboard: sessionUid={}", sessionUid);
        int maxCar = state.getNumActiveCars();
        Map<Integer, Integer> positionByCar = state.getLastCarPositionByCarIndex().entrySet().stream()
                .filter(e -> e.getKey() < maxCar)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (positionByCar.isEmpty()) {
            return List.of();
        }

        List<Lap> allLaps = lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(sessionUid);
        Map<Integer, Lap> lastLapByCar = lastCompletedLapPerCar(allLaps);

        Map<Integer, String> driverLabelByCar = sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(sessionUid)
                .stream()
                .collect(Collectors.toMap(sd -> sd.getCarIndex().intValue(), SessionDriver::getDriverLabel, (a, b) -> b));

        Map<Integer, Integer> bestFromSummaryByCar = new HashMap<>();
        for (SessionSummary ss : sessionSummaryRepository.findBySessionUid(sessionUid)) {
            if (ss.getCarIndex() != null && ss.getBestLapTimeMs() != null && ss.getBestLapTimeMs() > 0) {
                bestFromSummaryByCar.put(ss.getCarIndex().intValue(), ss.getBestLapTimeMs());
            }
        }

        List<Integer> carIndicesByPosition = positionByCar.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .toList();

        Map<Integer, Integer> totalMsByCar = new HashMap<>();
        for (Integer carIndex : carIndicesByPosition) {
            totalMsByCar.put(carIndex, LeaderboardTimingHelper.totalRaceTimeMs(carIndex, allLaps));
        }
        Integer leaderTotalMs = null;
        Integer leaderLastLapMs = null;
        if (!carIndicesByPosition.isEmpty()) {
            int leaderCar = carIndicesByPosition.get(0);
            int lt = totalMsByCar.getOrDefault(leaderCar, 0);
            if (lt > 0) {
                leaderTotalMs = lt;
            }
            Lap leaderLastLap = lastLapByCar.get(leaderCar);
            if (leaderLastLap != null && leaderLastLap.getLapTimeMs() != null && leaderLastLap.getLapTimeMs() > 0) {
                leaderLastLapMs = leaderLastLap.getLapTimeMs();
            }
        }

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int position = 1;
        for (Integer carIndex : carIndicesByPosition) {
            Lap lastLap = lastLapByCar.get(carIndex);
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
            String compound = TyreCompoundMapper.toDisplayString(state.getSnapshot(carIndex));
            if (GAP_EMPTY.equals(compound)) {
                Short actual = lastTyreCompoundState.get(sessionUid, carIndex);
                String fromActual = TyreCompoundMapper.toPersistedFromActualCompound(actual);
                if (fromActual != null) {
                    compound = fromActual;
                }
            }
            // Prefer driver name from Participants packet (game), then session_drivers (DB), then fallback
            String driverLabel = state.getParticipantName(carIndex);
            if (driverLabel == null || driverLabel.isBlank()) {
                driverLabel = driverLabelByCar.getOrDefault(carIndex, null);
            }
            if (driverLabel == null || driverLabel.isBlank()) {
                driverLabel = "Car " + carIndex;
            }

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

    private static Map<Integer, Lap> lastCompletedLapPerCar(List<Lap> allLaps) {
        Map<Integer, Lap> lastByCar = new HashMap<>();
        for (Lap lap : allLaps) {
            if (lap.getCarIndex() == null || lap.getLapTimeMs() == null || lap.getLapTimeMs() <= 0) {
                continue;
            }
            int car = lap.getCarIndex().intValue();
            Lap existing = lastByCar.get(car);
            if (existing == null || (lap.getLapNumber() != null && (existing.getLapNumber() == null || lap.getLapNumber() > existing.getLapNumber()))) {
                lastByCar.put(car, lap);
            }
        }
        return lastByCar;
    }

}
