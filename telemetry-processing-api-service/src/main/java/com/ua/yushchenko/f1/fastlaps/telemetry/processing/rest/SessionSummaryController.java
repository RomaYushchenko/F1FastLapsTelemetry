package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.SessionSummary;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
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
@RequestMapping("/api/sessions/{id}")
@RequiredArgsConstructor
public class SessionSummaryController {

    private final SessionSummaryRepository summaryRepository;
    private final SessionRepository sessionRepository;

    /**
     * GET /api/sessions/{id}/summary - Get session summary.
     * {@code id} can be the session UUID (public id) or the internal session_uid (Long).
     */
    @GetMapping("/summary")
    public ResponseEntity<SessionSummaryDto> getSummary(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return sessionRepository.findByPublicIdOrSessionUid(trimmedId)
                .map(session -> summaryRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex)
                        .map(this::toDto)
                        .orElse(emptySummaryDto()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Empty summary when session exists but no laps have been aggregated yet.
     */
    private static SessionSummaryDto emptySummaryDto() {
        return SessionSummaryDto.builder()
                .totalLaps(0)
                .bestLapTimeMs(null)
                .bestLapNumber(null)
                .bestSector1Ms(null)
                .bestSector2Ms(null)
                .bestSector3Ms(null)
                .build();
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
