package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErsPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapCornerDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SpeedTracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TracePointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TyreWearPointDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.LapQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for lap endpoints.
 * Thin: parameters → LapQueryService → DTO or 404 via exception handler.
 * See: implementation_phases.md Phase 3.1.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{id}")
@RequiredArgsConstructor
public class LapController {

    private final LapQueryService lapQueryService;

    @GetMapping("/laps")
    public ResponseEntity<List<LapResponseDto>> getLaps(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getLaps: id={}, carIndex={}", id, carIndex);
        List<LapResponseDto> list = lapQueryService.getLaps(id, carIndex);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/pace")
    public ResponseEntity<List<PacePointDto>> getPace(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getPace: id={}, carIndex={}", id, carIndex);
        List<PacePointDto> list = lapQueryService.getPace(id, carIndex);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/tyre-wear")
    public ResponseEntity<List<TyreWearPointDto>> getTyreWear(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getTyreWear: id={}, carIndex={}", id, carIndex);
        List<TyreWearPointDto> list = lapQueryService.getTyreWear(id, carIndex);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/laps/{lapNum}/trace")
    public ResponseEntity<List<TracePointDto>> getLapTrace(
            @PathVariable("id") String id,
            @PathVariable("lapNum") Integer lapNum,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getLapTrace: id={}, lapNum={}, carIndex={}", id, lapNum, carIndex);
        if (lapNum == null) {
            log.warn("lapNum is required");
            throw new IllegalArgumentException("lapNum is required");
        }
        List<TracePointDto> trace = lapQueryService.getLapTrace(id, lapNum, carIndex);
        return ResponseEntity.ok(trace);
    }

    @GetMapping("/laps/{lapNum}/corners")
    public ResponseEntity<List<LapCornerDto>> getLapCorners(
            @PathVariable("id") String id,
            @PathVariable("lapNum") Integer lapNum,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getLapCorners: id={}, lapNum={}, carIndex={}", id, lapNum, carIndex);
        if (lapNum == null) {
            log.warn("lapNum is required");
            throw new IllegalArgumentException("lapNum is required");
        }
        List<LapCornerDto> corners = lapQueryService.getCorners(id, lapNum, carIndex);
        return ResponseEntity.ok(corners);
    }

    @GetMapping("/laps/{lapNum}/speed-trace")
    public ResponseEntity<List<SpeedTracePointDto>> getLapSpeedTrace(
            @PathVariable("id") String id,
            @PathVariable("lapNum") Integer lapNum,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getLapSpeedTrace: id={}, lapNum={}, carIndex={}", id, lapNum, carIndex);
        if (lapNum == null) {
            log.warn("lapNum is required");
            throw new IllegalArgumentException("lapNum is required");
        }
        List<SpeedTracePointDto> trace = lapQueryService.getSpeedTrace(id, lapNum, carIndex);
        return ResponseEntity.ok(trace);
    }

    @GetMapping("/laps/{lapNum}/ers")
    public ResponseEntity<List<ErsPointDto>> getLapErs(
            @PathVariable("id") String id,
            @PathVariable("lapNum") Integer lapNum,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getLapErs: id={}, lapNum={}, carIndex={}", id, lapNum, carIndex);
        if (lapNum == null) {
            log.warn("lapNum is required");
            throw new IllegalArgumentException("lapNum is required");
        }
        List<ErsPointDto> ers = lapQueryService.getLapErs(id, lapNum, carIndex);
        return ResponseEntity.ok(ers);
    }

    @GetMapping("/sectors")
    public ResponseEntity<List<LapResponseDto>> getSectors(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getSectors: id={}, carIndex={}", id, carIndex);
        return ResponseEntity.ok(lapQueryService.getLaps(id, carIndex));
    }
}
