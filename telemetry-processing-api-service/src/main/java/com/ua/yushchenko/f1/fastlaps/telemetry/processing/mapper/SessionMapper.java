package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1SessionType;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.reference.F1Track;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Maps Session entity to REST DTO.
 * Session type and track display strings come from {@link F1SessionType} and {@link F1Track} (single place).
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
     * Convert Session entity to REST DTO with runtime state (ACTIVE/FINISHED).
     * Session type and track display names are resolved via F1SessionType and F1Track (telemetry-api-contracts).
     */
    public SessionDto toDto(Session session, SessionRuntimeState runtimeState) {
        if (session == null) {
            return null;
        }
        SessionState state = SessionState.FINISHED;
        if (runtimeState != null && runtimeState.isActive()) {
            state = SessionState.ACTIVE;
        }
        Short sessionTypeId = session.getSessionType();
        Integer trackId = session.getTrackId() != null ? session.getTrackId().intValue() : null;
        return SessionDto.builder()
                .id(toPublicIdString(session))
                .sessionDisplayName(session.getSessionDisplayName())
                .sessionType(sessionTypeId == null ? null : F1SessionType.fromCode(sessionTypeId).getDisplayName())
                .trackId(trackId)
                .trackDisplayName(trackId == null ? null : F1Track.fromId(trackId).getDisplayName())
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
