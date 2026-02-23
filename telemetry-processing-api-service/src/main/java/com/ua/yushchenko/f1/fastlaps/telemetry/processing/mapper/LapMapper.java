package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import org.springframework.stereotype.Component;

/**
 * Maps Lap and related entities to REST DTOs (laps, pace, trace, tyre wear).
 * See: implementation_phases.md Phase 1.1.
 */
@Component
public class LapMapper {

    public LapResponseDto toLapResponseDto(Lap lap) {
        if (lap == null) {
            return null;
        }
        return LapResponseDto.builder()
                .lapNumber(lap.getLapNumber() != null ? lap.getLapNumber().intValue() : 0)
                .lapTimeMs(lap.getLapTimeMs())
                .sector1Ms(lap.getSector1TimeMs())
                .sector2Ms(lap.getSector2TimeMs())
                .sector3Ms(lap.getSector3TimeMs())
                .isInvalid(lap.getIsInvalid() != null && lap.getIsInvalid())
                .positionAtLapStart(lap.getPositionAtLapStart())
                .build();
    }

    public PacePointDto toPacePointDto(Lap lap) {
        if (lap == null || lap.getLapTimeMs() == null || lap.getLapTimeMs() <= 0) {
            return null;
        }
        return PacePointDto.builder()
                .lapNumber(lap.getLapNumber() != null ? lap.getLapNumber().intValue() : 0)
                .lapTimeMs(lap.getLapTimeMs())
                .build();
    }

    public TracePointDto toTracePointDto(CarTelemetryRaw row) {
        if (row == null) {
            return null;
        }
        double distance = row.getLapDistanceM() != null ? row.getLapDistanceM().doubleValue() : 0.0;
        double throttle = row.getThrottle() != null ? row.getThrottle().doubleValue() : 0.0;
        double brake = row.getBrake() != null ? row.getBrake().doubleValue() : 0.0;
        return TracePointDto.builder()
                .distance(distance)
                .throttle(throttle)
                .brake(brake)
                .build();
    }

    /**
     * Maps raw telemetry row to speed-trace point (distance, speed).
     * Returns null if distance or speed is null (filtered out in service).
     */
    public SpeedTracePointDto toSpeedTracePointDto(CarTelemetryRaw row) {
        if (row == null || row.getLapDistanceM() == null || row.getSpeedKph() == null) {
            return null;
        }
        return SpeedTracePointDto.builder()
                .distanceM(row.getLapDistanceM())
                .speedKph(row.getSpeedKph().intValue())
                .build();
    }

    public TyreWearPointDto toTyreWearPointDto(TyreWearPerLap row) {
        if (row == null) {
            return null;
        }
        return TyreWearPointDto.builder()
                .lapNumber(row.getLapNumber() != null ? row.getLapNumber().intValue() : 0)
                .wearFL(row.getWearFL())
                .wearFR(row.getWearFR())
                .wearRL(row.getWearRL())
                .wearRR(row.getWearRR())
                .compound(row.getCompound() != null ? row.getCompound().intValue() : null)
                .build();
    }
}
