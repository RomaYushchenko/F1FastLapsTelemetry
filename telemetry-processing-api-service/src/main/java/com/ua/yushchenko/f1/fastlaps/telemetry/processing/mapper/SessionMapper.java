package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps Session entity to REST DTO.
 * Single source of truth for session id (public_id or session_uid) and session type display string.
 * See: implementation_phases.md Phase 1.1.
 */
@Component
public class SessionMapper {

    /**
     * Public id string for use in REST, WebSocket topic and client-facing APIs.
     * Prefer public_id (UUID) when present, otherwise session_uid.
     */
    public static String toPublicIdString(Session session) {
        if (session == null) {
            return null;
        }
        UUID publicId = session.getPublicId();
        return publicId != null ? publicId.toString() : String.valueOf(session.getSessionUid());
    }

    /**
     * Map F1 game session type id (m_sessionType, uint8) to display string.
     * Canonical mapping: {@code .github/docs/session_type_mapping.md}. Must match
     * SessionPacketParser.parseSessionType in udp-ingest-service.
     */
    public static String sessionTypeToDisplayString(Short sessionTypeId) {
        if (sessionTypeId == null) {
            return null;
        }
        int id = sessionTypeId.intValue() & 0xFF;
        return switch (id) {
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
            case 13 -> "SPRINT";
            case 14 -> "SPRINT_SHOOTOUT";
            case 15 -> "SPRINT";
            case 16 -> "SPRINT_SHOOTOUT";
            default -> "UNKNOWN";
        };
    }

    /**
     * Convert Session entity to REST DTO with runtime state (ACTIVE/FINISHED).
     */
    public SessionDto toDto(Session session, SessionRuntimeState runtimeState) {
        if (session == null) {
            return null;
        }
        SessionState state = SessionState.FINISHED;
        if (runtimeState != null && runtimeState.isActive()) {
            state = SessionState.ACTIVE;
        }
        return SessionDto.builder()
                .id(toPublicIdString(session))
                .sessionDisplayName(session.getSessionDisplayName())
                .sessionType(sessionTypeToDisplayString(session.getSessionType()))
                .trackId(session.getTrackId() != null ? session.getTrackId().intValue() : null)
                .trackLengthM(session.getTrackLengthM())
                .totalLaps(session.getTotalLaps() != null ? session.getTotalLaps().intValue() : null)
                .playerCarIndex(session.getPlayerCarIndex() != null ? session.getPlayerCarIndex().intValue() : null)
                .aiDifficulty(session.getAiDifficulty() != null ? session.getAiDifficulty().intValue() : null)
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .endReason(session.getEndReason())
                .state(state)
                .build();
    }
}
