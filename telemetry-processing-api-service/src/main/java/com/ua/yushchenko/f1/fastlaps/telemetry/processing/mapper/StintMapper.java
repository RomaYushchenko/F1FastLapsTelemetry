package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.StintDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps stint data (compound, start lap, lap count, laps for average) to StintDto.
 * avgLapTimeMs = average of valid lap times in stint; null if none. degradationIndicator = null in MVP.
 */
@Component
public class StintMapper {

    /**
     * Build StintDto for one stint. avgLapTimeMs is the average of valid lap times in the given laps; null if none.
     * degradationIndicator is null in MVP (UI shows "—").
     */
    public StintDto toDto(int stintIndex, Integer compound, int startLap, int lapCount, List<Lap> laps) {
        Integer avgLapTimeMs = null;
        if (laps != null && !laps.isEmpty()) {
            long sum = 0;
            int count = 0;
            for (Lap lap : laps) {
                if (lap.getLapTimeMs() != null && lap.getLapTimeMs() > 0) {
                    sum += lap.getLapTimeMs();
                    count++;
                }
            }
            if (count > 0) {
                avgLapTimeMs = (int) (sum / count);
            }
        }
        return StintDto.builder()
                .stintIndex(stintIndex)
                .compound(compound)
                .startLap(startLap)
                .lapCount(lapCount)
                .avgLapTimeMs(avgLapTimeMs)
                .degradationIndicator(null)
                .build();
    }
}
