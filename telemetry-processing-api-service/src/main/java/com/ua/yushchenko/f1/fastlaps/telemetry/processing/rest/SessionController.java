package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * GET /api/sessions - List all sessions.
     */
    @GetMapping
    public List<SessionDto> listSessions(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        log.debug("List sessions: offset={}, limit={}", offset, limit);
        
        return sessionRepository.findAll().stream()
                .skip(offset)
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/sessions/{sessionUid} - Get session details.
     */
    @GetMapping("/{sessionUid}")
    public ResponseEntity<SessionDto> getSession(@PathVariable Long sessionUid) {
        log.debug("Get session: sessionUid={}", sessionUid);
        
        return sessionRepository.findById(sessionUid)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
                .map(opt -> toDto(opt.get()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Convert Session entity to REST DTO.
     */
    private SessionDto toDto(Session session) {
        SessionRuntimeState runtimeState = stateManager.get(session.getSessionUid());
        
        SessionState state = SessionState.FINISHED;
        if (runtimeState != null && runtimeState.isActive()) {
            state = SessionState.ACTIVE;
        }
        
        return SessionDto.builder()
                .sessionUID(session.getSessionUid())
                .sessionType(session.getSessionType() != null ? String.valueOf(session.getSessionType()) : null)
                .trackId(session.getTrackId() != null ? session.getTrackId().intValue() : null)
                .trackLengthM(session.getTrackLengthM())
                .totalLaps(session.getTotalLaps() != null ? session.getTotalLaps().intValue() : null)
                .aiDifficulty(session.getAiDifficulty() != null ? session.getAiDifficulty().intValue() : null)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .endReason(session.getEndReason())
                .state(state)
                .build();
    }
}
