package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.service.SessionResolveService;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for session endpoints.
 * See: implementation_steps_plan.md § Етап 8.1-8.2.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRepository sessionRepository;
    private final SessionStateManager stateManager;
    private final SessionMapper sessionMapper;
    private final SessionResolveService sessionResolveService;

    /**
     * GET /api/sessions - List all sessions (includes legacy sessions without public_id).
     */
    @GetMapping
    public List<SessionDto> listSessions(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        log.debug("List sessions: offset={}, limit={}", offset, limit);

        int size = Math.max(1, Math.min(limit, 100));
        int page = offset / size;
        Pageable pageable = PageRequest.of(page, size);

        return sessionRepository.findAllByOrderByCreatedAtDesc(pageable).stream()
                .map(s -> sessionMapper.toDto(s, stateManager.get(s.getSessionUid())))
                .collect(Collectors.toList());
    }

    /**
     * GET /api/sessions/{id} - Get session details.
     * {@code id} can be the session UUID (public id) or the internal session_uid (Long).
     * Response uses the public UUID in the {@code id} field when present, else session_uid.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionDto> getSession(@PathVariable("id") String id) {
        String trimmedId = id != null ? id.trim() : "";
        if (trimmedId.isEmpty() || "active".equalsIgnoreCase(trimmedId)) {
            throw new SessionNotFoundException(trimmedId.isEmpty() ? "Session id is required" : "Use GET /api/sessions/active for active session");
        }
        log.debug("Get session: id={}", trimmedId);
        Session session = sessionResolveService.getSessionByPublicIdOrUid(trimmedId);
        SessionDto dto = sessionMapper.toDto(session, stateManager.get(session.getSessionUid()));
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/sessions/active - Get current active session.
     */
    @GetMapping("/active")
    public ResponseEntity<SessionDto> getActiveSession() {
        log.debug("Get active session");
        
        return stateManager.getAllActive().values().stream()
                .findFirst()
                .map(state -> sessionRepository.findById(state.getSessionUID()))
                .filter(opt -> opt.isPresent())
                .map(opt -> {
                    Session s = opt.get();
                    return sessionMapper.toDto(s, stateManager.get(s.getSessionUid()));
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
