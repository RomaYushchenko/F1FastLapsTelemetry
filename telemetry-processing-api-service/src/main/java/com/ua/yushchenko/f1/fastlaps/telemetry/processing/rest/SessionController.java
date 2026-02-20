package com.ua.yushchenko.f1.fastlaps.telemetry.processing.rest;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
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
                .map(this::toDto)
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
        if (trimmedId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if ("active".equalsIgnoreCase(trimmedId)) {
            return ResponseEntity.notFound().build();
        }
        log.debug("Get session: id={}", trimmedId);

        return sessionRepository.findByPublicIdOrSessionUid(trimmedId)
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
     * Single source of truth for session id: same id is used for GET /api/sessions/{id}, laps, summary.
     * Uses public_id (UUID) when present, otherwise session_uid (legacy rows).
     */
    private SessionDto toDto(Session session) {
        String id = session.getPublicId() != null
                ? session.getPublicId().toString()
                : String.valueOf(session.getSessionUid());
        SessionRuntimeState runtimeState = stateManager.get(session.getSessionUid());
        
        SessionState state = SessionState.FINISHED;
        if (runtimeState != null && runtimeState.isActive()) {
            state = SessionState.ACTIVE;
        }
        
        return SessionDto.builder()
                .id(id)
                .sessionType(sessionTypeToDisplayString(session.getSessionType()))
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

    /** Map F1 game session type id (0–12) to display string. Matches SessionPacketHandler.parseSessionType. */
    private static String sessionTypeToDisplayString(Short sessionTypeId) {
        if (sessionTypeId == null) return null;
        return switch (sessionTypeId.intValue()) {
            case 0 -> "UNKNOWN";
            case 1 -> "PRACTICE_1";
            case 2 -> "PRACTICE_2";
            case 3 -> "PRACTICE_3";
            case 4 -> "SHORT_PRACTICE";
            case 5 -> "QUALIFYING_1";
            case 6 -> "QUALIFYING_2";
            case 7 -> "QUALIFYING_3";
            case 8 -> "SHORT_QUALIFYING";
            case 9 -> "ONE_SHOT_QUALIFYING";
            case 10 -> "RACE";
            case 11 -> "RACE_2";
            case 12 -> "TIME_TRIAL";
            default -> "UNKNOWN";
        };
    }
}
