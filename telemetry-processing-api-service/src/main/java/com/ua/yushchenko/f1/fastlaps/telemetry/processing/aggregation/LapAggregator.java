package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates lap data: tracks sectors, finalizes laps.
 * See: implementation_steps_plan.md § Етап 6.2-6.3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LapAggregator {

    private final LapRepository lapRepository;
    private final SessionSummaryAggregator summaryAggregator;

    // Per session-car lap state
    private final Map<String, LapRuntimeState> lapStates = new ConcurrentHashMap<>();

    /**
     * Process lap data update.
     */
    public void processLapData(long sessionUid, short carIndex, LapDto lapDto) {
        String key = sessionUid + "-" + carIndex;
        LapRuntimeState state = lapStates.computeIfAbsent(key, 
            k -> new LapRuntimeState(sessionUid, carIndex));

        short lapNumber = (short) lapDto.getLapNumber();
        int sector = lapDto.getSector() != null ? lapDto.getSector() : 0;

        // Detect lap change
        if (lapNumber != state.getCurrentLapNumber()) {
            if (state.getCurrentLapNumber() > 0) {
                // Finalize previous lap before moving to next
                finalizeLap(state);
            }
            state.reset(lapNumber);
        }

        // Update current lap data
        state.setCurrentLapTimeMs(lapDto.getCurrentLapTimeMs());
        state.setInvalid(lapDto.isInvalid());
        state.setPenaltiesSeconds(lapDto.getPenaltiesSeconds() != null ? lapDto.getPenaltiesSeconds().shortValue() : (short) 0);

        // Track sector completion
        if (sector > state.getCurrentSector() && sector <= 3) {
            completeSector(state, sector, lapDto.getCurrentLapTimeMs());
        }

        // Check if lap is complete
        if (state.isComplete()) {
            finalizeLap(state);
            state.reset((short) (lapNumber + 1)); // Prepare for next lap
        }
    }

    /**
     * Complete a sector and store its time.
     */
    private void completeSector(LapRuntimeState state, int sector, Integer currentLapTime) {
        if (currentLapTime == null) {
            return;
        }

        int previousSectorTime = 0;
        if (sector == 2 && state.getSector1TimeMs() != null) {
            previousSectorTime = state.getSector1TimeMs();
        } else if (sector == 3 && state.getSector1TimeMs() != null && state.getSector2TimeMs() != null) {
            previousSectorTime = state.getSector1TimeMs() + state.getSector2TimeMs();
        }

        int sectorTime = currentLapTime - previousSectorTime;

        switch (sector) {
            case 1 -> state.setSector1TimeMs(sectorTime);
            case 2 -> state.setSector2TimeMs(sectorTime);
            case 3 -> state.setSector3TimeMs(sectorTime);
        }

        state.setCurrentSector(sector);
        log.debug("Sector {} completed: sessionUid={}, carIndex={}, lap={}, time={}ms",
                sector, state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber(), sectorTime);
    }

    /**
     * Finalize lap and persist to database.
     */
    @Transactional
    public void finalizeLap(LapRuntimeState state) {
        if (state.getCurrentLapNumber() == 0 || !state.isComplete()) {
            return; // Nothing to finalize
        }

        Lap lap = Lap.builder()
                .sessionUid(state.getSessionUid())
                .carIndex(state.getCarIndex())
                .lapNumber(state.getCurrentLapNumber())
                .lapTimeMs(state.getCurrentLapTimeMs())
                .sector1TimeMs(state.getSector1TimeMs())
                .sector2TimeMs(state.getSector2TimeMs())
                .sector3TimeMs(state.getSector3TimeMs())
                .isInvalid(state.isInvalid())
                .penaltiesSeconds(state.getPenaltiesSeconds())
                .endedAt(Instant.now())
                .build();

        lapRepository.save(lap);

        log.info("Lap finalized: sessionUid={}, carIndex={}, lap={}, time={}ms, invalid={}",
                state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber(),
                state.getCurrentLapTimeMs(), state.isInvalid());

        // Update session summary
        if (!state.isInvalid()) {
            summaryAggregator.updateWithLap(state.getSessionUid(), state.getCarIndex(), lap);
        }
    }

    /**
     * Finalize all pending laps for a session (called on session end).
     */
    public void finalizeAllLaps(long sessionUid) {
        lapStates.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionUid + "-"))
                .forEach(entry -> finalizeLap(entry.getValue()));
    }
}
