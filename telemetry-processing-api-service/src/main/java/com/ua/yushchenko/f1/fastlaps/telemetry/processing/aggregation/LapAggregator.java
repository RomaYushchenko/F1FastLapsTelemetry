package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.LapDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.builder.LapBuilder;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.LapId;
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
        // F1 m_sector: 0=S1, 1=S2, 2=S3 (current sector). Values 1 and 2 mean S1/S2 are complete for timing fields.
        int gameSector = lapDto.getSector() != null ? lapDto.getSector() : -1;
        int sectorJustCompleted = (gameSector == 1 || gameSector == 2) ? gameSector : -1;

        // Detect lap change: incoming packet is first packet of new lap; its lastLapTimeMs and sector times are for the lap we're finalizing
        if (lapNumber != state.getCurrentLapNumber()) {
            if (state.getCurrentLapNumber() > 0) {
                finalizeLap(state, lapDto.getLastLapTimeMs(), lapDto.getSector1TimeMs(), lapDto.getSector2TimeMs());
            }
            // Incoming packet is for the new lap; its carPosition is position at start of that lap
            if (lapDto.getCarPosition() != null && lapDto.getCarPosition() > 0) {
                state.setPositionAtLapStart(lapDto.getCarPosition());
            }
            state.reset(lapNumber);
        }

        // Update current lap data
        state.setCurrentLapTimeMs(lapDto.getCurrentLapTimeMs());
        if (lapDto.getCurrentLapTimeMs() != null && lapDto.getCurrentLapTimeMs() > 0) {
            if (state.getMaxCurrentLapTimeMs() == null
                    || lapDto.getCurrentLapTimeMs() > state.getMaxCurrentLapTimeMs()) {
                state.setMaxCurrentLapTimeMs(lapDto.getCurrentLapTimeMs());
            }
        }
        if (lapDto.getSector1TimeMs() != null && lapDto.getSector1TimeMs() > 0) {
            state.setLastPacketSector1TimeMs(lapDto.getSector1TimeMs());
        }
        if (lapDto.getSector2TimeMs() != null && lapDto.getSector2TimeMs() > 0) {
            state.setLastPacketSector2TimeMs(lapDto.getSector2TimeMs());
        }
        state.setInvalid(lapDto.isInvalid());
        state.setPenaltiesSeconds(lapDto.getPenaltiesSeconds() != null ? lapDto.getPenaltiesSeconds().shortValue() : (short) 0);

        // Track sector completion: game sends 1 when S1 done, 2 when S2 done; S3 is derived in finalizeLap.
        // Prefer official sector times from packet (m_sector1Time/m_sector2Time) so values match the game display.
        if (sectorJustCompleted > 0 && sectorJustCompleted > state.getCurrentSector()) {
            completeSector(state, sectorJustCompleted, lapDto);
        }

        // Check if lap is complete (all 3 sectors); no lastLapTimeMs yet, use current state
        if (state.isComplete()) {
            if (finalizeLap(state, null, null, null)) {
                state.reset((short) (lapNumber + 1)); // Prepare for next lap
            }
        }
    }

    /**
     * Complete a sector and store its time.
     * Prefer official sector times from the packet (m_sector1TimeMs / m_sector2TimeMs) when present,
     * so stored times match the game display; otherwise derive from currentLapTime.
     */
    private void completeSector(LapRuntimeState state, int sector, LapDto lapDto) {
        Integer sectorTime;
        if (sector == 1 && lapDto.getSector1TimeMs() != null && lapDto.getSector1TimeMs() > 0) {
            sectorTime = lapDto.getSector1TimeMs();
        } else if (sector == 2 && lapDto.getSector2TimeMs() != null && lapDto.getSector2TimeMs() > 0) {
            sectorTime = lapDto.getSector2TimeMs();
        } else {
            Integer currentLapTime = lapDto.getCurrentLapTimeMs();
            if (currentLapTime == null) {
                return;
            }
            int previousSectorTime = 0;
            if (sector == 2 && state.getSector1TimeMs() != null) {
                previousSectorTime = state.getSector1TimeMs();
            } else if (sector == 3 && state.getSector1TimeMs() != null && state.getSector2TimeMs() != null) {
                previousSectorTime = state.getSector1TimeMs() + state.getSector2TimeMs();
            }
            sectorTime = currentLapTime - previousSectorTime;
        }

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
     *
     * @return true if a lap row was written
     */
    @Transactional
    public boolean finalizeLap(LapRuntimeState state, Integer officialLapTimeMs, Integer officialSector1Ms, Integer officialSector2Ms) {
        if (state.getCurrentLapNumber() == 0) {
            return false;
        }
        int lapTimeMs = resolveLapTimeMsForFinalize(state, officialLapTimeMs);
        if (lapTimeMs <= 0) {
            return false;
        }

        Integer s1 = firstPositive(officialSector1Ms, state.getSector1TimeMs(), state.getLastPacketSector1TimeMs());
        Integer s2 = firstPositive(officialSector2Ms, state.getSector2TimeMs(), state.getLastPacketSector2TimeMs());
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
            return false;
        }

        persistLapRow(state, lapTimeMs, s1, s2, s3);
        return true;
    }

    private void persistLapRow(LapRuntimeState state, int lapTimeMs, int s1, int s2, int s3) {
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
                state.getPositionAtLapStart(),
                Instant.now()
        );
        lapRepository.save(lap);

        tyreWearRecorder.recordForLap(state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber());

        log.info("Lap finalized: sessionUid={}, carIndex={}, lap={}, time={}ms, invalid={}",
                state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber(),
                lapTimeMs, state.isInvalid());

        if (!state.isInvalid()) {
            summaryAggregator.updateWithLap(state.getSessionUid(), state.getCarIndex(), lap);
        }
    }

    /**
     * Last race lap often never gets a "next lap" packet before SEND; flush in-memory progress using max lap time and packet sectors.
     */
    private void finalizeLapRelaxedForSessionEnd(LapRuntimeState state) {
        int lapTimeMs = resolveLapTimeMsForFinalize(state, null);
        if (lapTimeMs <= 0) {
            log.debug("finalizeLapRelaxedForSessionEnd: no lap time, sessionUid={}, carIndex={}, lap={}",
                    state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber());
            return;
        }
        Integer s1o = firstPositive(state.getSector1TimeMs(), state.getLastPacketSector1TimeMs());
        Integer s2o = firstPositive(state.getSector2TimeMs(), state.getLastPacketSector2TimeMs());
        int s1 = s1o != null ? s1o : 0;
        int s2 = s2o != null ? s2o : 0;
        int s3 = lapTimeMs - s1 - s2;
        if (s3 < 0) {
            s1 = 0;
            s2 = 0;
            s3 = lapTimeMs;
            log.info("Session end lap flush: using single-sector split for sessionUid={}, carIndex={}, lap={}, time={}ms",
                    state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber(), lapTimeMs);
        } else {
            log.info("Session end lap flush: sessionUid={}, carIndex={}, lap={}, time={}ms",
                    state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber(), lapTimeMs);
        }
        persistLapRow(state, lapTimeMs, s1, s2, s3);
    }

    private static Integer firstPositive(Integer... values) {
        for (Integer v : values) {
            if (v != null && v > 0) {
                return v;
            }
        }
        return null;
    }

    private static int resolveLapTimeMsForFinalize(LapRuntimeState state, Integer officialLapTimeMs) {
        if (officialLapTimeMs != null && officialLapTimeMs > 0) {
            return officialLapTimeMs;
        }
        int best = 0;
        if (state.getCurrentLapTimeMs() != null && state.getCurrentLapTimeMs() > best) {
            best = state.getCurrentLapTimeMs();
        }
        if (state.getMaxCurrentLapTimeMs() != null && state.getMaxCurrentLapTimeMs() > best) {
            best = state.getMaxCurrentLapTimeMs();
        }
        return best;
    }

    private void finalizePendingLapOnSessionEnd(LapRuntimeState state) {
        if (state.getCurrentLapNumber() <= 0) {
            return;
        }
        LapId id = new LapId(state.getSessionUid(), state.getCarIndex(), state.getCurrentLapNumber());
        if (lapRepository.findById(id).filter(l -> l.getLapTimeMs() != null && l.getLapTimeMs() > 0).isPresent()) {
            return;
        }
        if (finalizeLap(state, null, null, null)) {
            return;
        }
        finalizeLapRelaxedForSessionEnd(state);
    }

    /**
     * Finalize all pending laps for a session (called on session end).
     */
    public void finalizeAllLaps(long sessionUid) {
        lapStates.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionUid + "-"))
                .map(Map.Entry::getValue)
                .forEach(this::finalizePendingLapOnSessionEnd);
    }
}
