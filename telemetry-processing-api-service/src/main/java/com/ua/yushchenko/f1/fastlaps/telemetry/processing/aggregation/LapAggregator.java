package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder.LapBuilder;
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
    private final TyreWearRecorder tyreWearRecorder;

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
        // F1 game m_sector = current sector: 0=in S1, 1=in S2, 2=in S3. So sector=1 means we just completed S1, sector=2 means we just completed S2.
        int gameSector = lapDto.getSector() != null ? lapDto.getSector() : -1;
        int sectorJustCompleted = (gameSector == 1 || gameSector == 2) ? gameSector : -1;

        // Detect lap change: incoming packet is first packet of new lap; its lastLapTimeMs and sector times are for the lap we're finalizing
        if (lapNumber != state.getCurrentLapNumber()) {
            if (state.getCurrentLapNumber() > 0) {
                finalizeLap(state, lapDto.getLastLapTimeMs(), lapDto.getSector1TimeMs(), lapDto.getSector2TimeMs());
            }
            state.reset(lapNumber);
        }

        // Update current lap data
        state.setCurrentLapTimeMs(lapDto.getCurrentLapTimeMs());
        state.setInvalid(lapDto.isInvalid());
        state.setPenaltiesSeconds(lapDto.getPenaltiesSeconds() != null ? lapDto.getPenaltiesSeconds().shortValue() : (short) 0);

        // Track sector completion: game sends 1 when S1 done, 2 when S2 done; S3 is derived in finalizeLap
        if (sectorJustCompleted > 0 && sectorJustCompleted > state.getCurrentSector()) {
            completeSector(state, sectorJustCompleted, lapDto.getCurrentLapTimeMs());
        }

        // Check if lap is complete (all 3 sectors); no lastLapTimeMs yet, use current state
        if (state.isComplete()) {
            finalizeLap(state, null, null, null);
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
     * Prefer official times from game (m_lastLapTimeInMS, m_sector1*, m_sector2* from first packet of next lap).
     */
    @Transactional
    public void finalizeLap(LapRuntimeState state, Integer officialLapTimeMs, Integer officialSector1Ms, Integer officialSector2Ms) {
        if (state.getCurrentLapNumber() == 0) {
            return;
        }
        int lapTimeMs = (officialLapTimeMs != null && officialLapTimeMs > 0)
                ? officialLapTimeMs
                : (state.getCurrentLapTimeMs() != null ? state.getCurrentLapTimeMs() : 0);

        // Prefer game sector times when available; otherwise use state (from currentLapTime at sector boundaries)
        Integer s1 = (officialSector1Ms != null && officialSector1Ms > 0) ? officialSector1Ms : state.getSector1TimeMs();
        Integer s2 = (officialSector2Ms != null && officialSector2Ms > 0) ? officialSector2Ms : state.getSector2TimeMs();
        Integer s3 = state.getSector3TimeMs();
        if (s3 == null && s1 != null && s2 != null && lapTimeMs > 0) {
            int derived = lapTimeMs - s1 - s2;
            if (derived >= 0) {
                s3 = derived;
                log.debug("Derived sector3: sessionUid={}, lap={}, sector3Ms={}",
                        state.getSessionUid(), state.getCurrentLapNumber(), derived);
            }
        }
        if (s1 == null || s2 == null || s3 == null) {
            return; // Still missing sector times
        }

        Lap lap = LapBuilder.build(
                state.getSessionUid(),
                state.getCarIndex(),
                state.getCurrentLapNumber(),
                lapTimeMs,
                s1,
                s2,
                s3,
                state.isInvalid(),
                state.getPenaltiesSeconds(),
                Instant.now()
        );
        lapRepository.save(lap);

        tyreWearRecorder.recordForLap(state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber());

        log.info("Lap finalized: sessionUid={}, carIndex={}, lap={}, time={}ms, invalid={}",
                state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber(),
                lapTimeMs, state.isInvalid());

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
                .forEach(entry -> finalizeLap(entry.getValue(), null, null, null));
    }
}
