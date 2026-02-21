package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionResolveService")
class SessionResolveServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionResolveService service;

    @Test
    @DisplayName("getSessionByPublicIdOrUid кидає виняток коли id null")
    void getSessionByPublicIdOrUid_throws_whenIdNull() {
        // Act & Assert
        assertThatThrownBy(() -> service.getSessionByPublicIdOrUid(null))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session id is required");
    }

    @Test
    @DisplayName("getSessionByPublicIdOrUid кидає виняток коли id порожній")
    void getSessionByPublicIdOrUid_throws_whenIdBlank() {
        // Act & Assert
        assertThatThrownBy(() -> service.getSessionByPublicIdOrUid("   "))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session id is required");
    }

    @Test
    @DisplayName("getSessionByPublicIdOrUid кидає виняток коли сесія не знайдена")
    void getSessionByPublicIdOrUid_throws_whenSessionNotFound() {
        // Arrange
        when(sessionRepository.findByPublicIdOrSessionUid("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getSessionByPublicIdOrUid("unknown"))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Session not found: unknown");

        verify(sessionRepository).findByPublicIdOrSessionUid("unknown");
    }

    @Test
    @DisplayName("getSessionByPublicIdOrUid повертає Session коли знайдено")
    void getSessionByPublicIdOrUid_returnsSession_whenFound() {
        // Arrange
        Session session = session();
        when(sessionRepository.findByPublicIdOrSessionUid(SESSION_PUBLIC_ID_STR)).thenReturn(Optional.of(session));

        // Act
        Session result = service.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(result).isSameAs(session);
        verify(sessionRepository).findByPublicIdOrSessionUid(SESSION_PUBLIC_ID_STR);
    }

    @Test
    @DisplayName("getSessionByPublicIdOrUid обрізає пробіли у введенні")
    void getSessionByPublicIdOrUid_trimsInput() {
        // Arrange
        Session session = session();
        when(sessionRepository.findByPublicIdOrSessionUid(SESSION_PUBLIC_ID_STR)).thenReturn(Optional.of(session));

        // Act
        Session result = service.getSessionByPublicIdOrUid("  " + SESSION_PUBLIC_ID_STR + "  ");

        // Assert
        assertThat(result).isSameAs(session);
        verify(sessionRepository).findByPublicIdOrSessionUid(SESSION_PUBLIC_ID_STR);
    }
}
