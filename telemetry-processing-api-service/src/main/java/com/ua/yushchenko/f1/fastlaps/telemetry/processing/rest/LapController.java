package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.LapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.PacePointDto;
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
        List<LapResponseDto> list = lapQueryService.getLaps(id, carIndex);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/pace")
    public ResponseEntity<List<PacePointDto>> getPace(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        List<PacePointDto> list = lapQueryService.getPace(id, carIndex);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/tyre-wear")
    public ResponseEntity<List<TyreWearPointDto>> getTyreWear(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        List<TyreWearPointDto> list = lapQueryService.getTyreWear(id, carIndex);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/laps/{lapNum}/trace")
    public ResponseEntity<List<TracePointDto>> getLapTrace(
            @PathVariable("id") String id,
            @PathVariable("lapNum") Integer lapNum,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        if (lapNum == null) {
            throw new IllegalArgumentException("lapNum is required");
        }
        List<TracePointDto> trace = lapQueryService.getLapTrace(id, lapNum, carIndex);
        return ResponseEntity.ok(trace);
    }

    @GetMapping("/sectors")
    public ResponseEntity<List<LapResponseDto>> getSectors(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        return ResponseEntity.ok(lapQueryService.getLaps(id, carIndex));
    }
}
