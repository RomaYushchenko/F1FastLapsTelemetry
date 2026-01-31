package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for session summary endpoint.
 * See: implementation_steps_plan.md § Етап 8.5.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions/{sessionUid}")
@RequiredArgsConstructor
public class SessionSummaryController {

    private final SessionSummaryRepository summaryRepository;

    /**
     * GET /api/sessions/{sessionUid}/summary - Get session summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<SessionSummaryDto> getSummary(
            @PathVariable Long sessionUid,
            @RequestParam(defaultValue = "0") Short carIndex
    ) {
        log.debug("Get summary: sessionUid={}, carIndex={}", sessionUid, carIndex);
        
        return summaryRepository.findBySessionUidAndCarIndex(sessionUid, carIndex)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convert SessionSummary entity to REST DTO.
     */
    private SessionSummaryDto toDto(SessionSummary summary) {
        return SessionSummaryDto.builder()
                .totalLaps(summary.getTotalLaps() != null ? summary.getTotalLaps().intValue() : null)
                .bestLapTimeMs(summary.getBestLapTimeMs())
                .bestLapNumber(summary.getBestLapNumber() != null ? summary.getBestLapNumber().intValue() : null)
                .bestSector1Ms(summary.getBestSector1Ms())
                .bestSector2Ms(summary.getBestSector2Ms())
                .bestSector3Ms(summary.getBestSector3Ms())
                .build();
    }
}
