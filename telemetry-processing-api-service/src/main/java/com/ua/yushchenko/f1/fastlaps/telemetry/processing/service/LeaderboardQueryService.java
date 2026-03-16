package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.CarPositionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LeaderboardEntryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionDriver;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionDriverRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds live leaderboard for the active session: positions from state, last lap from DB,
 * compound from CarStatus snapshot, driver label from session_drivers (fallback "Car N").
 * Block E — Live leaderboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardQueryService {

    private static final String GAP_LEAD = "LEAD";
    private static final String GAP_EMPTY = "—";

    private final SessionStateManager stateManager;
    private final LapRepository lapRepository;
    private final SessionDriverRepository sessionDriverRepository;

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
        Map<Integer, Integer> positionByCar = state.getLastCarPositionByCarIndex();
        if (positionByCar.isEmpty()) {
            return List.of();
        }

        List<Lap> allLaps = lapRepository.findBySessionUidOrderByCarIndexAscLapNumberAsc(sessionUid);
        Map<Integer, Lap> lastLapByCar = lastCompletedLapPerCar(allLaps);

        Map<Integer, String> driverLabelByCar = sessionDriverRepository.findBySessionUidOrderByCarIndexAsc(sessionUid)
                .stream()
                .collect(Collectors.toMap(sd -> sd.getCarIndex().intValue(), SessionDriver::getDriverLabel, (a, b) -> b));

        Integer leaderLastLapMs = null;
        List<Integer> carIndicesByPosition = positionByCar.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .toList();

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int position = 1;
        for (Integer carIndex : carIndicesByPosition) {
            Lap lastLap = lastLapByCar.get(carIndex);
            if (leaderLastLapMs == null && lastLap != null && lastLap.getLapTimeMs() != null && lastLap.getLapTimeMs() > 0) {
                leaderLastLapMs = lastLap.getLapTimeMs();
            }
            String gap = formatGap(position, leaderLastLapMs, lastLap);
            String compound = compoundFromSnapshot(state.getSnapshot(carIndex));
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

    private static String formatGap(int position, Integer leaderLastLapMs, Lap carLastLap) {
        if (position == 1) {
            return GAP_LEAD;
        }
        if (leaderLastLapMs == null || carLastLap == null || carLastLap.getLapTimeMs() == null || carLastLap.getLapTimeMs() <= 0) {
            return GAP_EMPTY;
        }
        int deltaMs = carLastLap.getLapTimeMs() - leaderLastLapMs;
        if (deltaMs <= 0) {
            return GAP_LEAD;
        }
        return "+" + formatLapTimeForGap(deltaMs);
    }

    private static String formatLapTimeForGap(int ms) {
        int sec = ms / 1000;
        int frac = (ms % 1000) / 10;
        if (sec >= 60) {
            int min = sec / 60;
            sec = sec % 60;
            return String.format("%d:%02d.%02d", min, sec, frac);
        }
        return String.format("%d.%02d", sec, frac);
    }

    /**
     * Map F1 visual tyre compound code to S/M/H. Default H if unknown.
     */
    private static String compoundFromSnapshot(SessionRuntimeState.CarSnapshot snapshot) {
        if (snapshot == null || snapshot.getVisualTyreCompound() == null) {
            return "—";
        }
        int code = snapshot.getVisualTyreCompound();
        if (code <= 17) {
            return "S";
        }
        if (code <= 19) {
            return "M";
        }
        return "H";
    }
}
