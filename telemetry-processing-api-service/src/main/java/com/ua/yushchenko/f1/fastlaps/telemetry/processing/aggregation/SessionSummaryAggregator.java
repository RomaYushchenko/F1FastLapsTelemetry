package com.ua.yushchenko.f1.fastlaps.telemetry.processing.aggregation;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates session summary: best lap, best sectors, total laps.
 * See: implementation_steps_plan.md § Етап 6.4.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSummaryAggregator {

    private final SessionSummaryRepository summaryRepository;

    /**
     * Update session summary with a completed lap.
     */
    @Transactional
    public void updateWithLap(long sessionUid, short carIndex, Lap lap) {
        SessionSummary summary = summaryRepository.findBySessionUidAndCarIndex(sessionUid, carIndex)
                .orElse(SessionSummary.builder()
                        .sessionUid(sessionUid)
                        .carIndex(carIndex)
                        .totalLaps((short) 0)
                        .build());

        // Update total laps
        summary.setTotalLaps((short) (summary.getTotalLaps() + 1));

        // Update best lap time
        if (lap.getLapTimeMs() != null && !lap.getIsInvalid()) {
            if (summary.getBestLapTimeMs() == null || lap.getLapTimeMs() < summary.getBestLapTimeMs()) {
                summary.setBestLapTimeMs(lap.getLapTimeMs());
                summary.setBestLapNumber(lap.getLapNumber());
                log.info("New best lap: sessionUid={}, carIndex={}, lap={}, time={}ms",
                        sessionUid, carIndex, lap.getLapNumber(), lap.getLapTimeMs());
            }
        }

        // Update best sector 1
        if (lap.getSector1TimeMs() != null && !lap.getIsInvalid()) {
            if (summary.getBestSector1Ms() == null || lap.getSector1TimeMs() < summary.getBestSector1Ms()) {
                summary.setBestSector1Ms(lap.getSector1TimeMs());
                log.debug("New best S1: sessionUid={}, carIndex={}, time={}ms",
                        sessionUid, carIndex, lap.getSector1TimeMs());
            }
        }

        // Update best sector 2
        if (lap.getSector2TimeMs() != null && !lap.getIsInvalid()) {
            if (summary.getBestSector2Ms() == null || lap.getSector2TimeMs() < summary.getBestSector2Ms()) {
                summary.setBestSector2Ms(lap.getSector2TimeMs());
                log.debug("New best S2: sessionUid={}, carIndex={}, time={}ms",
                        sessionUid, carIndex, lap.getSector2TimeMs());
            }
        }

        // Update best sector 3
        if (lap.getSector3TimeMs() != null && !lap.getIsInvalid()) {
            if (summary.getBestSector3Ms() == null || lap.getSector3TimeMs() < summary.getBestSector3Ms()) {
                summary.setBestSector3Ms(lap.getSector3TimeMs());
                log.debug("New best S3: sessionUid={}, carIndex={}, time={}ms",
                        sessionUid, carIndex, lap.getSector3TimeMs());
            }
        }

        summaryRepository.save(summary);
    }
}
