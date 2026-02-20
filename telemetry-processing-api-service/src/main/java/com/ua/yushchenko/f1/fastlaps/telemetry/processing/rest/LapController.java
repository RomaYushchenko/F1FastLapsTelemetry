package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.LapMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.CarTelemetryRawRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.LapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.TyreWearPerLapRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionResolveService;
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
    private final SessionResolveService sessionResolveService;
    private final CarTelemetryRawRepository carTelemetryRawRepository;
    private final TyreWearPerLapRepository tyreWearPerLapRepository;
    private final LapMapper lapMapper;

    /**
     * GET /api/sessions/{id}/laps - Get laps for session.
     * {@code id} can be the session UUID (public id) or the internal session_uid (Long).
     */
    @GetMapping("/laps")
    public ResponseEntity<List<LapResponseDto>> getLaps(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(id != null ? id.trim() : "");
        List<LapResponseDto> list = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toLapResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
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
        Session session = sessionResolveService.getSessionByPublicIdOrUid(id != null ? id.trim() : "");
        List<PacePointDto> list = lapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toPacePointDto)
                .filter(p -> p != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
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
        Session session = sessionResolveService.getSessionByPublicIdOrUid(id != null ? id.trim() : "");
        List<TyreWearPointDto> list = tyreWearPerLapRepository.findBySessionUidAndCarIndexOrderByLapNumberAsc(session.getSessionUid(), carIndex)
                .stream()
                .map(lapMapper::toTyreWearPointDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
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
        if (lapNum == null) {
            throw new IllegalArgumentException("lapNum is required");
        }
        Session session = sessionResolveService.getSessionByPublicIdOrUid(id != null ? id.trim() : "");
        List<TracePointDto> trace = carTelemetryRawRepository
                .findBySessionUidAndCarIndexAndLapNumberOrderByFrameIdentifierAsc(
                        session.getSessionUid(), carIndex, lapNum.shortValue())
                .stream()
                .map(lapMapper::toTracePointDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(trace);
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

}
