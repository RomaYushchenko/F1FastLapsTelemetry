package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionEventDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionEventsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for session events (GET /api/sessions/{id}/events).
 * Block E — Session events.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionEventsController {

    private final SessionEventsQueryService sessionEventsQueryService;

    @GetMapping("/{id}/events")
    public ResponseEntity<List<SessionEventDto>> getEvents(
            @PathVariable("id") String sessionId,
            @RequestParam(name = "fromLap", required = false) Short fromLap,
            @RequestParam(name = "toLap", required = false) Short toLap,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        log.debug("Get events: sessionId={}, fromLap={}, toLap={}, limit={}", sessionId, fromLap, toLap, limit);
        List<SessionEventDto> events = sessionEventsQueryService.getEvents(sessionId, fromLap, toLap, limit);
        return ResponseEntity.ok(events);
    }
}
