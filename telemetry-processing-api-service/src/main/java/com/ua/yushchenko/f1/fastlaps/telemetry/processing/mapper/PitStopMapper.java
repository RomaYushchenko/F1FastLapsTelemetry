package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PitStopDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import org.springframework.stereotype.Component;

/**
 * Maps pit stop data (in-lap, out-lap, compounds) to PitStopDto.
 * Used by PitStopQueryService. No business logic; pure transformation.
 */
@Component
public class PitStopMapper {

    /**
     * Build PitStopDto from in-lap, out-lap and compound codes.
     * lapNumber is the out-lap number; pitDurationMs is null in MVP.
     */
    public PitStopDto toDto(Lap inLap, Lap outLap, Integer compoundIn, Integer compoundOut) {
        if (outLap == null) {
            return null;
        }
        int lapNumber = outLap.getLapNumber() != null ? outLap.getLapNumber().intValue() : 0;
        Integer inLapTimeMs = inLap != null ? inLap.getLapTimeMs() : null;
        Integer outLapTimeMs = outLap.getLapTimeMs();
        return PitStopDto.builder()
                .lapNumber(lapNumber)
                .inLapTimeMs(inLapTimeMs)
                .pitDurationMs(null)
                .outLapTimeMs(outLapTimeMs)
                .compoundIn(compoundIn)
                .compoundOut(compoundOut)
                .build();
    }
}
