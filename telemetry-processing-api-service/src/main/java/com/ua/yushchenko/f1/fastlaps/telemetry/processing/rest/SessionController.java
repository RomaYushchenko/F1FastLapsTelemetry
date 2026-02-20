package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public List<SessionDto> listSessions(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        log.debug("List sessions: offset={}, limit={}", offset, limit);
        return sessionQueryService.listSessions(offset, limit);
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
}
