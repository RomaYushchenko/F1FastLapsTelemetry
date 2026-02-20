package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionSummaryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionSummaryMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionSummaryRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionResolveService;
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
    private final SessionResolveService sessionResolveService;
    private final SessionSummaryMapper sessionSummaryMapper;

    /**
     * GET /api/sessions/{id}/summary - Get session summary.
     * {@code id} can be the session UUID (public id) or the internal session_uid (Long).
     */
    @GetMapping("/summary")
    public ResponseEntity<SessionSummaryDto> getSummary(
            @PathVariable("id") String id,
            @RequestParam(name = "carIndex", defaultValue = "0") Short carIndex
    ) {
        Session session = sessionResolveService.getSessionByPublicIdOrUid(id != null ? id.trim() : "");
        SessionSummaryDto dto = summaryRepository.findBySessionUidAndCarIndex(session.getSessionUid(), carIndex)
                .map(sessionSummaryMapper::toDto)
                .orElse(SessionSummaryMapper.emptySummaryDto());
        return ResponseEntity.ok(dto);
    }
}
