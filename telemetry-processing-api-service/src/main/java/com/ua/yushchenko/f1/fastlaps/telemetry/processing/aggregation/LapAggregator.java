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

        // Track sector completion: game sends 1 when S1 done, 2 when S2 done; S3 is derived in finalizeLap
        if (sectorJustCompleted > 0 && sectorJustCompleted > state.getCurrentSector()) {
            completeSector(state, sectorJustCompleted, lapDto.getCurrentLapTimeMs());
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
     * F1 game m_sector is current sector (0/1/2); we only get completion for S1 and S2.
     * Derive sector3 from lap time when we have s1 and s2 so the lap can be saved.
     */
    @Transactional
    public void finalizeLap(LapRuntimeState state) {
        if (state.getCurrentLapNumber() == 0) {
            return;
        }
        // Derive sector 3 time when game only sent sectors 0,1,2 (no "sector 3" transition)
        if (state.getSector3TimeMs() == null
                && state.getSector1TimeMs() != null
                && state.getSector2TimeMs() != null
                && state.getCurrentLapTimeMs() != null) {
            int s3 = state.getCurrentLapTimeMs() - state.getSector1TimeMs() - state.getSector2TimeMs();
            if (s3 >= 0) {
                state.setSector3TimeMs(s3);
                log.debug("Derived sector3: sessionUid={}, lap={}, sector3Ms={}",
                        state.getSessionUid(), state.getCurrentLapNumber(), s3);
            }
        }
        if (!state.isComplete()) {
            return; // Still missing sector times
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

        tyreWearRecorder.recordForLap(state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber());

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
