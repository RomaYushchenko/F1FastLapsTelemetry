package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionSummaryQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for session summary endpoint.
 * Thin: parameters → SessionSummaryQueryService → DTO or 404 via exception handler.
 * See: implementation_phases.md Phase 3.1.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{id}")
@RequiredArgsConstructor
public class SessionSummaryController {

    private final SessionSummaryQueryService sessionSummaryQueryService;

    @GetMapping("/summary")
    public ResponseEntity<SessionSummaryDto> getSummary(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        log.debug("getSummary: id={}, carIndex={}", id, carIndex);
        SessionSummaryDto dto = sessionSummaryQueryService.getSummary(id, carIndex);
        return ResponseEntity.ok(dto);
    }
}
