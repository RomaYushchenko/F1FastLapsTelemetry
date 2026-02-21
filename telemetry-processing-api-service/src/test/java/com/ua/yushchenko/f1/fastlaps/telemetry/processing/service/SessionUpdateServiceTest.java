package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.SessionDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.mapper.SessionMapper;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionUpdateService")
class SessionUpdateServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionStateManager stateManager;
    @Spy
    private SessionMapper sessionMapper = new SessionMapper();

    @InjectMocks
    private SessionUpdateService service;

    @Test
    @DisplayName("updateDisplayName оновлює назву та повертає DTO")
    void updateDisplayName_updatesNameAndReturnsDto() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(stateManager.get(SESSION_UID)).thenReturn(runtimeStateActive());
        String newName = "Monaco Race 2026";

        // Act
        SessionDto result = service.updateDisplayName(SESSION_PUBLIC_ID_STR, newName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(SESSION_PUBLIC_ID_STR);
        assertThat(result.getSessionDisplayName()).isEqualTo(newName);
        verify(sessionRepository).save(session);
        assertThat(session.getSessionDisplayName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("updateDisplayName кидає SessionNotFoundException коли сесія не знайдена")
    void updateDisplayName_throwsWhenSessionNotFound() {
        // Arrange
        when(sessionResolveService.getSessionByPublicIdOrUid("unknown-id"))
                .thenThrow(new SessionNotFoundException("Session not found: unknown-id"));

        // Act & Assert
        assertThatThrownBy(() -> service.updateDisplayName("unknown-id", "Some Name"))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("unknown-id");
        verify(sessionResolveService).getSessionByPublicIdOrUid("unknown-id");
    }

    @Test
    @DisplayName("updateDisplayName trim-ує пробіли")
    void updateDisplayName_trimsWhitespace() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(stateManager.get(SESSION_UID)).thenReturn(runtimeStateActive());

        // Act
        SessionDto result = service.updateDisplayName(SESSION_PUBLIC_ID_STR, "  Trimmed  ");

        // Assert
        assertThat(session.getSessionDisplayName()).isEqualTo("Trimmed");
        assertThat(result.getSessionDisplayName()).isEqualTo("Trimmed");
    }
}
