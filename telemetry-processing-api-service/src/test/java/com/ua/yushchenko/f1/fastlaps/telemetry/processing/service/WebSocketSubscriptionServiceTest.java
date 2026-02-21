package com.ua.yushchenko.f1.fastlaps.telemetry.processing.service;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.rest.ErrorCode;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.exception.SessionNotFoundException;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity.Session;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionRuntimeState;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.state.SessionStateManager;
import com.ua.yushchenko.f1.fastlaps.telemetry.processing.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.ua.yushchenko.f1.fastlaps.telemetry.processing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSubscriptionService")
class WebSocketSubscriptionServiceTest {

    @Mock
    private SessionResolveService sessionResolveService;
    @Mock
    private SessionStateManager sessionStateManager;
    @Mock
    private WebSocketSessionManager wsSessionManager;

    @InjectMocks
    private WebSocketSubscriptionService service;

    @Test
    @DisplayName("subscribe повертає error коли sessionId порожній")
    void subscribe_returnsError_whenSessionIdBlank() {
        // Act
        var result = service.subscribe("ws-1", "   ");

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.INVALID_SUBSCRIPTION);
        assertThat(result.message()).contains("Missing session id");
    }

    @Test
    @DisplayName("subscribe повертає error коли sessionId null")
    void subscribe_returnsError_whenSessionIdNull() {
        // Act
        var result = service.subscribe("ws-1", null);

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.INVALID_SUBSCRIPTION);
    }

    @Test
    @DisplayName("subscribe повертає error коли сесія не знайдена")
    void subscribe_returnsError_whenSessionNotFound() {
        // Arrange
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR))
                .thenThrow(new SessionNotFoundException("Session not found: " + SESSION_PUBLIC_ID_STR));

        // Act
        var result = service.subscribe("ws-1", SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("subscribe повертає error коли сесія не активна")
    void subscribe_returnsError_whenSessionNotActive() {
        // Arrange
        Session session = session();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(null);

        // Act
        var result = service.subscribe("ws-1", SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.SESSION_NOT_ACTIVE);
    }

    @Test
    @DisplayName("subscribe повертає error коли стан сесії terminal")
    void subscribe_returnsError_whenSessionStateTerminal() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateTerminal();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(state);

        // Act
        var result = service.subscribe("ws-1", SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ErrorCode.SESSION_NOT_ACTIVE);
    }

    @Test
    @DisplayName("subscribe повертає ok та реєструє сесію коли успішно")
    void subscribe_returnsOk_andRegistersSession() {
        // Arrange
        Session session = session();
        SessionRuntimeState state = runtimeStateActive();
        when(sessionResolveService.getSessionByPublicIdOrUid(SESSION_PUBLIC_ID_STR)).thenReturn(session);
        when(sessionStateManager.get(SESSION_UID)).thenReturn(state);

        // Act
        var result = service.subscribe("ws-1", SESSION_PUBLIC_ID_STR);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.errorCode()).isNull();
        verify(wsSessionManager).subscribe("ws-1", SESSION_UID);
    }
}
