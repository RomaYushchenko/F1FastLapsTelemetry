package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1SessionType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListFilter;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionListResult;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionQueryService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for session endpoints.
 * Thin: parameters → SessionQueryService → DTO or 404 via exception handler.
 * See: implementation_phases.md Phase 3.1.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionQueryService sessionQueryService;
    private final SessionUpdateService sessionUpdateService;

    /** Header name for total count (for pagination "Showing X–Y of Z"). */
    public static final String HEADER_TOTAL_COUNT = "X-Total-Count";

    @GetMapping
    public ResponseEntity<List<SessionDto>> listSessions(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "sessionType", required = false) String sessionType,
            @RequestParam(name = "trackId", required = false) Integer trackId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo
    ) {
        log.debug("List sessions: offset={}, limit={}, sessionType={}, trackId={}, search={}, sort={}, state={}, dateFrom={}, dateTo={}",
                offset, limit, sessionType, trackId, search, sort, state, dateFrom, dateTo);

        Integer sessionTypeCode = null;
        if (sessionType != null && !sessionType.isBlank()) {
            try {
                sessionTypeCode = F1SessionType.valueOf(sessionType.trim().toUpperCase()).getCode();
            } catch (IllegalArgumentException ignored) {
                log.debug("Unknown sessionType param: {}, ignoring", sessionType);
            }
        }

        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);

        SessionListFilter filter = SessionListFilter.builder()
                .offset(offset)
                .limit(limit)
                .sessionType(sessionTypeCode)
                .trackId(trackId)
                .search(search)
                .sort(sort != null && !sort.isBlank() ? sort : "startedAt_desc")
                .state(state)
                .dateFrom(from)
                .dateTo(to)
                .build();

        SessionListResult result = sessionQueryService.listSessions(filter);
        return ResponseEntity.ok()
                .header(HEADER_TOTAL_COUNT, String.valueOf(result.getTotal()))
                .body(result.getList());
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionDto> getSession(@PathVariable("id") String id) {
        log.debug("Get session: id={}", id);
        SessionDto dto = sessionQueryService.getSession(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/active")
    public ResponseEntity<SessionDto> getActiveSession() {
        log.debug("Get active session");
        return sessionQueryService.getActiveSession()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Update session display name. Returns updated SessionDto; 404 if session not found.
     * Plan: 03-session-page.md Etap 1.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SessionDto> updateSessionDisplayName(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateSessionDisplayNameRequest request
    ) {
        log.debug("Patch session display name: id={}", id);
        SessionDto dto = sessionUpdateService.updateDisplayName(id, request.getSessionDisplayName());
        return ResponseEntity.ok(dto);
    }
}
