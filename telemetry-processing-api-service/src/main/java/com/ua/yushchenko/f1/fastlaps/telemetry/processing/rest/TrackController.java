package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.TrackCornerMapResponseDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.TrackCornerMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for track-level endpoints (corner maps).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Slf4j
@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackCornerMapService trackCornerMapService;

    @GetMapping("/{trackId}/corner-maps/latest")
    public ResponseEntity<TrackCornerMapResponseDto> getLatestCornerMap(
            @PathVariable("trackId") Short trackId,
            @RequestParam("trackLengthM") Integer trackLengthM
    ) {
        log.debug("getLatestCornerMap: trackId={}, trackLengthM={}", trackId, trackLengthM);
        if (trackLengthM == null) {
            log.warn("trackLengthM is required");
            throw new IllegalArgumentException("trackLengthM is required");
        }
        Optional<TrackCornerMapResponseDto> dto = trackCornerMapService.getLatestMapDto(trackId, trackLengthM);
        return dto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
