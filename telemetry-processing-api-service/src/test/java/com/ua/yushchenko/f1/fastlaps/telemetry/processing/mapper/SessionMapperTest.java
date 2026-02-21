package com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionMapper")
class SessionMapperTest {

    private SessionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SessionMapper();
    }

    @Test
    @DisplayName("toPublicIdString повертає null коли session null")
    void toPublicIdString_returnsNull_whenSessionIsNull() {
        // Act
        String result = SessionMapper.toPublicIdString(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toPublicIdString повертає publicId коли він заданий")
    void toPublicIdString_returnsPublicId_whenPresent() {
        // Arrange
        Session session = session();

        // Act
        String result = SessionMapper.toPublicIdString(session);

        // Assert
        assertThat(result).isEqualTo(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("toPublicIdString повертає sessionUid коли publicId null")
    void toPublicIdString_returnsSessionUid_whenPublicIdNull() {
        // Arrange
        Session session = sessionWithoutPublicId();

        // Act
        String result = SessionMapper.toPublicIdString(session);

        // Assert
        assertThat(result).isEqualTo(String.valueOf(SESSION_UID));
    }

    @Test
    @DisplayName("sessionTypeToDisplayString повертає null коли sessionType null")
    void sessionTypeToDisplayString_returnsNull_whenNull() {
        // Act
        String result = SessionMapper.sessionTypeToDisplayString(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("sessionTypeToDisplayString мапить відомі типи сесій")
    void sessionTypeToDisplayString_mapsKnownTypes() {
        // Act & Assert
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 0)).isEqualTo("UNKNOWN");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 1)).isEqualTo("PRACTICE_1");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 2)).isEqualTo("PRACTICE_2");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 10)).isEqualTo("RACE");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 12)).isEqualTo("TIME_TRIAL");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 13)).isEqualTo("SPRINT");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 14)).isEqualTo("SPRINT_SHOOTOUT");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 15)).isEqualTo("SPRINT");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 16)).isEqualTo("SPRINT_SHOOTOUT");
    }

    @Test
    @DisplayName("sessionTypeToDisplayString повертає UNKNOWN для невідомого id")
    void sessionTypeToDisplayString_returnsUnknown_forUnknownId() {
        // Act & Assert
        assertThat(SessionMapper.sessionTypeToDisplayString((short) 99)).isEqualTo("UNKNOWN");
        assertThat(SessionMapper.sessionTypeToDisplayString((short) -1)).isEqualTo("UNKNOWN"); // 255 as byte
    }

    @Test
    @DisplayName("toDto повертає null коли session null")
    void toDto_returnsNull_whenSessionNull() {
        // Act
        SessionDto result = mapper.toDto(null, runtimeStateActive());

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDto мапить у ACTIVE state коли runtimeState активний")
    void toDto_mapsToActiveState_whenRuntimeStateActive() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();

        // Act
        SessionDto dto = mapper.toDto(session, state);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(dto.getSessionType()).isEqualTo("RACE");
        assertThat(dto.getTrackId()).isEqualTo(TRACK_ID);
        assertThat(dto.getTrackLengthM()).isEqualTo(TRACK_LENGTH_M);
        assertThat(dto.getTotalLaps()).isEqualTo((int) TOTAL_LAPS);
        assertThat(dto.getAiDifficulty()).isEqualTo((int) AI_DIFFICULTY);
        assertThat(dto.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(dto.getEndedAt()).isEqualTo(ENDED_AT);
        assertThat(dto.getEndReason()).isEqualTo(END_REASON);
        assertThat(dto.getState()).isEqualTo(SessionState.ACTIVE);
    }

    @Test
    @DisplayName("toDto мапить у FINISHED state коли runtimeState null")
    void toDto_mapsToFinishedState_whenRuntimeStateNull() {
        // Arrange
        Session session = session();

        // Act
        SessionDto dto = mapper.toDto(session, null);

        // Assert
        assertThat(dto.getState()).isEqualTo(SessionState.FINISHED);
    }

    @Test
    @DisplayName("toDto мапить у FINISHED state коли runtimeState terminal")
    void toDto_mapsToFinishedState_whenRuntimeStateTerminal() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateTerminal();

        // Act
        SessionDto dto = mapper.toDto(session, state);

        // Assert
        assertThat(dto.getState()).isEqualTo(SessionState.FINISHED);
    }

    @Test
    @DisplayName("toDto коректно обробляє null trackId, totalLaps, aiDifficulty")
    void toDto_handlesNullTrackIdAndTotalLaps() {
        // Arrange
        Session session = session();
        session.setTrackId(null);
        session.setTotalLaps(null);
        session.setAiDifficulty(null);

        // Act
        SessionDto dto = mapper.toDto(session, runtimeStateActive());

        // Assert
        assertThat(dto.getTrackId()).isNull();
        assertThat(dto.getTotalLaps()).isNull();
        assertThat(dto.getAiDifficulty()).isNull();
    }
}
