package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.CarTelemetryRaw;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Lap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.TyreWearPerLap;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for lap endpoints.
 * See: implementation_steps_plan.md § Етап 8.3-8.4.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{id}")
@RequiredArgsConstructor
public class LapController {

    private final LapRepository lapRepository;
    private final SessionRepository sessionRepository;
    private final CarTelemetryRawRepository carTelemetryRawRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;

    /**
     * GET /api/sessions/{id}/laps - Get laps for session.
     * {@code id} can be the session UUID (public id) or the internal session_uid (Long).
     */
    @GetMapping("/laps")
    public ResponseEntity<List<LapResponseDto>> getLaps(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return sessionRepository.findByPublicIdOrSessionUid(trimmedId)
                .map(session -> lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                        .stream()
                        .map(LapController::toDto)
                        .collect(Collectors.toList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/sessions/{id}/pace - Get pace data for chart (lap number + lap time per lap).
     * Returns 200 with empty list when session has no laps or no lap times.
     */
    @GetMapping("/pace")
    public ResponseEntity<List<PacePointDto>> getPace(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return sessionRepository.findByPublicIdOrSessionUid(trimmedId)
                .map(session -> lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                        .stream()
                        .filter(lap -> lap.getLapTimeMs() != null && lap.getLapTimeMs() > 0)
                        .map(lap -> PacePointDto.builder()
                                .lapNumber(lap.getLapNumber())
                                .lapTimeMs(lap.getLapTimeMs())
                                .build())
                        .collect(Collectors.toList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/sessions/{id}/tyre-wear - Get tyre wear per lap for chart (lap number + wear % per wheel).
     * Returns 200 with empty list when session has no tyre wear data.
     */
    @GetMapping("/tyre-wear")
    public ResponseEntity<List<TyreWearPointDto>> getTyreWear(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return sessionRepository.findByPublicIdOrSessionUid(trimmedId)
                .map(session -> tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                        .stream()
                        .map(LapController::toTyreWearPointDto)
                        .collect(Collectors.toList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/sessions/{id}/laps/{lapNum}/trace - Get throttle/brake trace for a lap.
     * Returns samples from car_telemetry_raw (distance, throttle, brake). 200 + empty list if no data.
     */
    @GetMapping("/laps/{lapNum}/trace")
    public ResponseEntity<List<TracePointDto>> getLapTrace(
            @PathVariable("id") String id,
            @PathVariable("lapNum") Integer lapNum,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty() || lapNum == null) {
            return ResponseEntity.notFound().build();
        }
        Short lapNumShort = lapNum.shortValue();
        return sessionRepository.findByPublicIdOrSessionUid(trimmedId)
                .map(session -> {
                    List<CarTelemetryRaw> rows = carTelemetryRawRepository
                            .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                                    session.getSessionUid(), carIndex, lapNumShort);
                    List<TracePointDto> trace = rows.stream()
                            .map(LapController::toTracePointDto)
                            .collect(Collectors.toList());
                    return ResponseEntity.<List<TracePointDto>>ok(trace);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private static TracePointDto toTracePointDto(CarTelemetryRaw row) {
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
     * GET /api/sessions/{id}/sectors - Get sectors (same as laps for MVP).
     */
    @GetMapping("/sectors")
    public ResponseEntity<List<LapResponseDto>> getSectors(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        return getLaps(id, carIndex);
    }

    private static TyreWearPointDto toTyreWearPointDto(TyreWearPerLap row) {
        return TyreWearPointDto.builder()
                .lapNumber(row.getLapNumber() != null ? row.getLapNumber().intValue() : 0)
                .wearFL(row.getWearFL())
                .wearFR(row.getWearFR())
                .wearRL(row.getWearRL())
                .wearRR(row.getWearRR())
                .build();
    }

    /**
     * Convert Lap entity to REST DTO.
     */
    private static LapResponseDto toDto(Lap lap) {
        return LapResponseDto.builder()
                .lapNumber(lap.getLapNumber())
                .lapTimeMs(lap.getLapTimeMs())
                .sector1Ms(lap.getSector1TimeMs())
                .sector2Ms(lap.getSector2TimeMs())
                .sector3Ms(lap.getSector3TimeMs())
                .isInvalid(lap.getIsInvalid() != null && lap.getIsInvalid())
                .build();
    }
}
