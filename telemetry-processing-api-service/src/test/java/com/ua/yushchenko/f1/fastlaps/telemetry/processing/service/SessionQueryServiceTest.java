package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionQueryService")
class SessionQueryServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionStateManager stateManager;
    @Mock
    private SessionResolveService sessionResolveService;

    private SessionQueryService service;

    @BeforeEach
    void setUp() {
        SessionMapper sessionMapper = new SessionMapper();
        service = new SessionQueryService(sessionRepository, stateManager, sessionMapper, sessionResolveService);
    }

    @Test
    @DisplayName("listSessions повертає змаплені DTO")
    void listSessions_returnsMappedDtos() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(sessionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(session));
        when(stateManager.get(SESSION_UID)).thenReturn(state);

        // Act
        List<SessionDto> result = service.listSessions(0, 50);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(result.get(0).getSessionType()).isEqualTo("RACE");
        verify(sessionRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50));
    }

    @Test
    @DisplayName("listSessions обмежує limit до 100")
    void listSessions_clampsLimitTo100() {
        // Arrange
        when(sessionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

        // Act
        service.listSessions(0, 200);

        // Assert
        verify(sessionRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100));
    }

    @Test
    @DisplayName("getSession повертає DTO коли сесія знайдена")
    void getSession_returnsDto_whenFound() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(stateManager.get(SESSION_UID)).thenReturn(state);

        // Act
        SessionDto dto = service.getSession(SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(dto.getState()).isEqualTo(com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionState.ACTIVE);
    }

    @Test
    @DisplayName("getSession кидає виняток коли id дорівнює 'active'")
    void getSession_throws_whenIdIsActive() {
        // Act & Assert
        assertThatThrownBy(() -> service.getSession("active"))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Use GET /api/sessions/active");
    }

    @Test
    @DisplayName("getSession кидає виняток коли id порожній")
    void getSession_throws_whenIdBlank() {
        // Act & Assert
        assertThatThrownBy(() -> service.getSession("   "))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session id is required");
    }

    @Test
    @DisplayName("getActiveSession повертає empty коли немає активних")
    void getActiveSession_returnsEmpty_whenNoActive() {
        // Arrange
        when(stateManager.getAllActive()).thenReturn(Map.of());

        // Act
        var result = service.getActiveSession();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getActiveSession повертає DTO коли є одна активна сесія")
    void getActiveSession_returnsDto_whenOneActive() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(stateManager.getAllActive()).thenReturn(Map.of(SESSION_UID, state));
        when(sessionRepository.findById(SESSION_UID)).thenReturn(Optional.of(session));
        when(stateManager.get(SESSION_UID)).thenReturn(state);

        // Act
        Optional<SessionDto> result = service.getActiveSession();

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getTopicIdForSession повертає empty коли sessionUid null")
    void getTopicIdForSession_returnsEmpty_whenSessionUidNull() {
        // Act
        var result = service.getTopicIdForSession(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopicIdForSession повертає empty коли сесія не знайдена")
    void getTopicIdForSession_returnsEmpty_whenSessionNotFound() {
        // Arrange
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        var result = service.getTopicIdForSession(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getTopicIdForSession повертає publicId коли сесія знайдена")
    void getTopicIdForSession_returnsPublicId_whenFound() {
        // Arrange
        Session session = session();
        when(sessionRepository.findById(SESSION_UID)).thenReturn(Optional.of(session));

        // Act
        var result = service.getTopicIdForSession(SESSION_UID);

        // Assert
        assertThat(result).contains(SESSION_PUBLIC_ID_STR);
    }
}
